package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.control.PerformanceRunUiController;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.performance.core.runtime.PerformanceThreadFactory;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse;
import com.laker.postman.performance.master.PerformanceWorkerHttpClient;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
final class PerformanceRemoteRunControlSupport {
    private static final long POLL_INTERVAL_MS = 1_000L;
    private static final long TIMEOUT_MS = 86_400_000L;

    private final BooleanSupplier runningSupplier;
    private final Consumer<Boolean> runningSetter;
    private final PerformanceRunUiController runUiController;
    private final PerformanceReportPanel performanceReportPanel;
    private final Runnable clearCachedPerformanceResultsAction;
    private final PerformanceWorkerAssignmentPlanner assignmentPlanner = new PerformanceWorkerAssignmentPlanner();
    private final PerformanceWorkerHttpClient workerClient = new PerformanceWorkerHttpClient();
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private volatile String currentRunId = "";
    private volatile List<PerformanceWorkerEndpoint> currentWorkers = List.of();

    Thread startRun(PerformanceRunPlan runPlan,
                    List<PerformanceWorkerEndpoint> workers,
                    JLabel progressLabel) {
        if (runningSupplier.getAsBoolean()) {
            return null;
        }
        List<PerformanceWorkerEndpoint> safeWorkers = workers == null ? List.of() : List.copyOf(workers);
        if (safeWorkers.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_REQUIRED));
            return null;
        }

        stopping.set(false);
        showAssetWarningIfNeeded(runPlan);
        runningSetter.accept(true);
        runUiController.markRunning();
        clearCachedPerformanceResultsAction.run();
        runUiController.initializeProgress(progressLabel, safeWorkers.size());

        Thread thread = PerformanceThreadFactory.newDaemonThread(
                "PerformanceRemoteRun",
                () -> runToCompletion(runPlan, safeWorkers, progressLabel)
        );
        thread.start();
        return thread;
    }

    void stopRun() {
        if (!runningSupplier.getAsBoolean()) {
            return;
        }
        stopping.set(true);
        sendStopAsync(currentRunId, currentWorkers);
    }

    private void runToCompletion(PerformanceRunPlan runPlan,
                                 List<PerformanceWorkerEndpoint> workers,
                                 JLabel progressLabel) {
        long startTime = System.currentTimeMillis();
        String runId = "gui-" + startTime;
        currentRunId = runId;
        currentWorkers = workers;
        try {
            submitRun(runPlan, workers, runId);
            if (stopping.get()) {
                sendStopAsync(runId, workers);
            }
            waitForWorkers(workers, runId, progressLabel);
            PerformanceJsonReport report = collectReport(workers, runId);
            finishRun(report, workers.size(), System.currentTimeMillis() - startTime);
        } catch (Exception ex) {
            sendStopAsync(runId, workers);
            log.error("Remote performance run failed", ex);
            finishFailed(ex);
        } finally {
            currentRunId = "";
            currentWorkers = List.of();
        }
    }

    private void submitRun(PerformanceRunPlan runPlan,
                           List<PerformanceWorkerEndpoint> workers,
                           String runId) throws Exception {
        // GUI remote 只做 JMeter 风格的控制面分发；plan 中的本地资产路径由用户提前放到每台 worker。
        List<PerformanceWorkerAssignment> assignments = assignmentPlanner.plan(runPlan, workers, runId);
        for (int i = 0; i < workers.size(); i++) {
            workerClient.submitRun(workers.get(i), PerformanceWorkerRunRequest.builder()
                    .runId(runId)
                    .plan(runPlan)
                    .assignment(assignments.get(i))
                    .build());
        }
    }

    private void showAssetWarningIfNeeded(PerformanceRunPlan runPlan) {
        if (runPlan != null && !runPlan.getAssets().isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_REMOTE_ASSETS_WARNING,
                    runPlan.getAssets().size()
            ));
        }
    }

    private void waitForWorkers(List<PerformanceWorkerEndpoint> workers,
                                String runId,
                                JLabel progressLabel) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (true) {
            int done = 0;
            for (PerformanceWorkerEndpoint worker : workers) {
                PerformanceWorkerRunStatusResponse status = workerClient.status(worker, runId);
                if (isTerminal(status.getStatus())) {
                    done++;
                }
            }
            runUiController.updateProgressAsync(progressLabel, done, workers.size());
            if (done == workers.size()) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException("Timed out waiting for workers");
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
    }

    private PerformanceJsonReport collectReport(List<PerformanceWorkerEndpoint> workers,
                                                String runId) throws Exception {
        List<PerformanceJsonReport> reports = new ArrayList<>();
        String status = stopping.get() ? PerformanceRunStatus.STOPPED : PerformanceRunStatus.SUCCESS;
        for (PerformanceWorkerEndpoint worker : workers) {
            PerformanceWorkerRunResultResponse response = workerClient.result(worker, runId);
            if (PerformanceRunStatus.FAILED.equals(response.getStatus())) {
                status = PerformanceRunStatus.FAILED;
            } else if (PerformanceRunStatus.STOPPED.equals(response.getStatus())
                    && !PerformanceRunStatus.FAILED.equals(status)) {
                status = PerformanceRunStatus.STOPPED;
            }
            if (response.getReport() != null) {
                reports.add(response.getReport());
            } else if (response.getError() != null && !response.getError().isBlank()) {
                reports.add(workerErrorReport(worker, runId, response));
            }
        }
        return PerformanceJsonReportSummaryMapper.merge(runId, "gui-master", status, "GUI", reports);
    }

    private PerformanceJsonReport workerErrorReport(PerformanceWorkerEndpoint worker,
                                                    String runId,
                                                    PerformanceWorkerRunResultResponse response) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(runId)
                        .source(endpointLabel(worker))
                        .status(response.getStatus())
                        .error(response.getError())
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
    }

    private void finishRun(PerformanceJsonReport report, int workerCount, long elapsedMs) {
        SwingUtilities.invokeLater(() -> {
            runningSetter.accept(false);
            runUiController.markIdle();
            performanceReportPanel.updateReport(report);
            PerformanceJsonReportSummary summary = report.getSummary();
            if (PerformanceRunStatus.STOPPED.equals(report.getMetadata().getStatus())) {
                NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_MSG_STOPPED));
            } else if (PerformanceRunStatus.FAILED.equals(report.getMetadata().getStatus())) {
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_MSG_FAILED,
                        report.getMetadata().getError()));
            } else {
                NotificationUtil.showSuccess(I18nUtil.getMessage(
                        MessageKeys.PERFORMANCE_REMOTE_MSG_COMPLETED,
                        workerCount,
                        summary.getTotalRequests(),
                        summary.getSuccessRequests(),
                        elapsedMs / 1000.0
                ));
            }
        });
    }

    private void finishFailed(Exception ex) {
        SwingUtilities.invokeLater(() -> {
            runningSetter.accept(false);
            runUiController.markIdle();
            NotificationUtil.showError(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_REMOTE_MSG_FAILED,
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
            ));
        });
    }

    private void sendStopAsync(String runId, List<PerformanceWorkerEndpoint> workers) {
        if (runId == null || runId.isBlank() || workers == null || workers.isEmpty()) {
            return;
        }
        Thread thread = PerformanceThreadFactory.newDaemonThread("PerformanceRemoteStop", () -> {
            for (PerformanceWorkerEndpoint worker : workers) {
                try {
                    workerClient.stop(worker, runId);
                } catch (Exception ex) {
                    log.warn("Failed to stop remote worker {}", endpointLabel(worker), ex);
                }
            }
        });
        thread.start();
    }

    private boolean isTerminal(String status) {
        return PerformanceRunStatus.isTerminal(status);
    }

    private String endpointLabel(PerformanceWorkerEndpoint endpoint) {
        return endpoint == null ? "" : endpoint.getHost() + ":" + endpoint.getPort();
    }
}

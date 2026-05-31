package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.control.PerformanceRunUiController;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceWorkerResultDetailDisplayMapper;
import com.laker.postman.panel.performance.result.PerformanceTrendView;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportTrendWindowSampler;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.performance.core.runtime.PerformanceThreadFactory;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerResultDetail;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunDetailsResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse;
import com.laker.postman.performance.master.PerformanceWorkerHttpClient;
import com.laker.postman.performance.master.PerformanceWorkerReportCollector;
import com.laker.postman.performance.master.PerformanceWorkerReportCollector.PerformanceWorkerReportResult;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.Millisecond;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

@Slf4j
@RequiredArgsConstructor
final class PerformanceRemoteRunControlSupport {
    private static final long POLL_INTERVAL_MS = 1_000L;
    private static final long TIMEOUT_MS = 86_400_000L;

    private final BooleanSupplier runningSupplier;
    private final Consumer<Boolean> runningSetter;
    private final PerformanceRunUiController runUiController;
    private final PerformanceReportPanel performanceReportPanel;
    private final PerformanceResultTablePanel performanceResultTablePanel;
    private final PerformanceTrendView performanceTrendPanel;
    private final Runnable clearCachedPerformanceResultsAction;
    private final Runnable showReportTabAction;
    private final BooleanSupplier trendEnabledSupplier;
    private final BooleanSupplier reportRealtimeEnabledSupplier;
    private final LongSupplier trendSamplingIntervalMsSupplier;
    private final PerformanceWorkerAssignmentPlanner assignmentPlanner = new PerformanceWorkerAssignmentPlanner();
    private final PerformanceWorkerHttpClient workerClient = new PerformanceWorkerHttpClient();
    private final PerformanceWorkerReportCollector reportCollector = new PerformanceWorkerReportCollector(workerClient);
    private final PerformanceJsonReportTrendWindowSampler trendWindowSampler = new PerformanceJsonReportTrendWindowSampler();
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private volatile String currentRunId = "";
    private volatile List<PerformanceWorkerEndpoint> currentWorkers = List.of();
    private volatile int currentTotalUsers;
    private volatile long lastTrendSampleAtMs;
    private volatile PerformanceJsonReport lastLiveReport;

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
        currentTotalUsers = 0;
        lastTrendSampleAtMs = 0L;
        lastLiveReport = null;
        trendWindowSampler.reset(0L);
        runUiController.initializeProgress(progressLabel, 0);

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
            int totalUsers = submitRun(runPlan, workers, runId);
            currentTotalUsers = totalUsers;
            resetRemoteTrendSamplingWindow();
            runUiController.initializeProgress(progressLabel, totalUsers);
            notifyStarted(workers.size(), runId);
            updateRemoteProgress(progressLabel, RemoteProgressSnapshot.empty(totalUsers));
            if (stopping.get()) {
                sendStopAsync(runId, workers);
            }
            waitForWorkers(workers, runId, progressLabel, totalUsers);
            PerformanceJsonReport report = collectReport(workers, runId);
            List<PerformanceWorkerResultDetail> details = collectDetails(workers, runId);
            finishRun(report, details, workers.size(), totalUsers, progressLabel);
        } catch (Exception ex) {
            sendStopAsync(runId, workers);
            log.error("Remote performance run failed", ex);
            finishFailed(ex, progressLabel);
        } finally {
            currentRunId = "";
            currentWorkers = List.of();
        }
    }

    private int submitRun(PerformanceRunPlan runPlan,
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
        return totalAssignedUsers(assignments);
    }

    private void resetRemoteTrendSamplingWindow() {
        long now = System.currentTimeMillis();
        lastTrendSampleAtMs = now;
        trendWindowSampler.reset(now);
    }

    private void notifyStarted(int workerCount, String runId) {
        SwingUtilities.invokeLater(() -> NotificationUtil.showInfo(I18nUtil.getMessage(
                MessageKeys.PERFORMANCE_REMOTE_MSG_STARTED,
                workerCount,
                runId
        )));
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
                                JLabel progressLabel,
                                int totalAssignedUsers) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (true) {
            RemoteProgressSnapshot progress = pollProgress(workers, runId, totalAssignedUsers);
            updateRemoteProgress(progressLabel, progress);
            updateLiveViews(progress, false);
            if (progress.completedWorkers() == workers.size()) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException("Timed out waiting for workers");
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
    }

    private RemoteProgressSnapshot pollProgress(List<PerformanceWorkerEndpoint> workers,
                                                String runId,
                                                int totalAssignedUsers) throws Exception {
        int done = 0;
        int activeUsers = 0;
        int totalUsers = 0;
        int activeWebSocketConnections = 0;
        int activeSseStreams = 0;
        long totalRequests = 0;
        long failedRequests = 0;
        List<PerformanceJsonReport> reports = new ArrayList<>();
        boolean includeReport = shouldIncludeStatusReport();
        for (PerformanceWorkerEndpoint worker : workers) {
            PerformanceWorkerRunStatusResponse status = workerClient.status(worker, runId, includeReport);
            if (isTerminal(status.getStatus())) {
                done++;
            }
            activeUsers += Math.max(0, status.getActiveUsers());
            totalUsers += Math.max(0, status.getTotalUsers());
            activeWebSocketConnections += Math.max(0, status.getActiveWebSocketConnections());
            activeSseStreams += Math.max(0, status.getActiveSseStreams());
            totalRequests += Math.max(0L, status.getTotalRequests());
            failedRequests += Math.max(0L, status.getFailedRequests());
            if (status.getReport() != null) {
                reports.add(status.getReport());
            }
        }
        int resolvedTotalUsers = totalUsers > 0 ? totalUsers : Math.max(0, totalAssignedUsers);
        PerformanceJsonReport report = reports.isEmpty()
                ? null
                : PerformanceJsonReportSummaryMapper.merge(
                runId,
                "gui-master",
                stopping.get() ? PerformanceRunStatus.STOPPING : PerformanceRunStatus.RUNNING,
                "GUI",
                reports
        );
        rememberLastLiveReport(report);
        return new RemoteProgressSnapshot(
                activeUsers,
                resolvedTotalUsers,
                activeWebSocketConnections,
                activeSseStreams,
                totalRequests,
                failedRequests,
                done,
                report
        );
    }

    private PerformanceJsonReport collectReport(List<PerformanceWorkerEndpoint> workers,
                                                String runId) throws Exception {
        List<PerformanceJsonReport> reports = new ArrayList<>();
        String status = stopping.get() ? PerformanceRunStatus.STOPPED : PerformanceRunStatus.SUCCESS;
        for (PerformanceWorkerEndpoint worker : workers) {
            PerformanceWorkerReportResult response = reportCollector.collect(worker, runId);
            if (PerformanceRunStatus.FAILED.equals(response.status())) {
                status = PerformanceRunStatus.FAILED;
            } else if (PerformanceRunStatus.STOPPED.equals(response.status())
                    && !PerformanceRunStatus.FAILED.equals(status)) {
                status = PerformanceRunStatus.STOPPED;
            }
            if (response.report() != null) {
                reports.add(response.report());
            } else if (response.error() != null && !response.error().isBlank()) {
                reports.add(workerErrorReport(worker, runId, response));
            }
        }
        if (!PerformanceWorkerReportCollector.hasAnyReportData(reports)
                && PerformanceWorkerReportCollector.hasReportData(lastLiveReport)) {
            // 远程实时刷新已经拿到过有效报表时，最终 result 偶发空报表不能覆盖用户看到的结果。
            reports.add(lastLiveReport);
            log.warn("使用运行中最后一次有效报表兜底 master 最终报表: runId={}", runId);
        }
        return PerformanceJsonReportSummaryMapper.merge(runId, "gui-master", status, "GUI", reports);
    }

    private List<PerformanceWorkerResultDetail> collectDetails(List<PerformanceWorkerEndpoint> workers, String runId) {
        List<PerformanceWorkerResultDetail> details = new ArrayList<>();
        for (PerformanceWorkerEndpoint worker : workers) {
            try {
                PerformanceWorkerRunDetailsResponse response = workerClient.details(worker, runId);
                details.addAll(response.getDetails());
            } catch (Exception ex) {
                log.warn("Failed to collect remote worker result details: worker={}, runId={}",
                        endpointLabel(worker), runId, ex);
            }
        }
        return details;
    }

    private PerformanceJsonReport workerErrorReport(PerformanceWorkerEndpoint worker,
                                                    String runId,
                                                    PerformanceWorkerReportResult response) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(runId)
                        .source(endpointLabel(worker))
                        .status(response.status())
                        .error(response.error())
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
    }

    private void finishRun(PerformanceJsonReport report,
                           List<PerformanceWorkerResultDetail> details,
                           int workerCount,
                           int totalUsers,
                           JLabel progressLabel) {
        SwingUtilities.invokeLater(() -> {
            runningSetter.accept(false);
            runUiController.markIdle();
            if (showReportTabAction != null) {
                showReportTabAction.run();
            }
            performanceReportPanel.updateReport(report);
            updateResultDetails(details);
            if (PerformanceRunStatus.STOPPED.equals(report.getMetadata().getStatus())) {
                updateCompletionProgress(progressLabel, totalUsers);
                updateFinalTrend(report, totalUsers, workerCount);
                NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_MSG_STOPPED));
            } else if (PerformanceRunStatus.FAILED.equals(report.getMetadata().getStatus())) {
                updateCompletionProgress(progressLabel, totalUsers);
                updateFinalTrend(report, totalUsers, workerCount);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_MSG_FAILED,
                        report.getMetadata().getError()));
            } else {
                PerformanceJsonReportSummary summary = report.getSummary();
                updateCompletionProgress(progressLabel, totalUsers);
                updateFinalTrend(report, totalUsers, workerCount);
                NotificationUtil.showSuccess(I18nUtil.getMessage(
                        MessageKeys.PERFORMANCE_REMOTE_MSG_COMPLETED,
                        workerCount,
                        summary.getTotalRequests(),
                        summary.getSuccessRequests()
                ));
            }
        });
    }

    private void updateResultDetails(List<PerformanceWorkerResultDetail> details) {
        if (performanceResultTablePanel == null || details == null || details.isEmpty()) {
            return;
        }
        for (PerformanceWorkerResultDetail detail : details) {
            performanceResultTablePanel.addResult(
                    PerformanceWorkerResultDetailDisplayMapper.toResultNodeInfo(detail),
                    false
            );
        }
        performanceResultTablePanel.flushPendingResults();
    }

    private void finishFailed(Exception ex, JLabel progressLabel) {
        int totalUsers = currentTotalUsers;
        SwingUtilities.invokeLater(() -> {
            runningSetter.accept(false);
            runUiController.markIdle();
            runUiController.updateProgressAsync(progressLabel, 0, totalUsers);
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

    private void rememberLastLiveReport(PerformanceJsonReport report) {
        if (PerformanceWorkerReportCollector.hasReportData(report)) {
            lastLiveReport = report;
        }
    }

    private void updateRemoteProgress(JLabel progressLabel, RemoteProgressSnapshot progress) {
        if (progressLabel == null) {
            return;
        }
        runUiController.updateProgressAsync(progressLabel, progress.activeUsers(), progress.totalUsers());
    }

    private void updateCompletionProgress(JLabel progressLabel, int totalUsers) {
        runUiController.updateProgressAsync(progressLabel, 0, Math.max(0, totalUsers));
    }

    private void updateLiveViews(RemoteProgressSnapshot progress, boolean forceTrend) {
        if (progress == null) {
            return;
        }
        if (isReportRealtimeEnabled() && progress.report() != null) {
            SwingUtilities.invokeLater(() -> performanceReportPanel.updateReport(progress.report()));
        }
        if (shouldUpdateTrend(forceTrend)) {
            updateTrendView(progress);
        }
    }

    private boolean shouldUpdateTrend(boolean forceTrend) {
        if (!isTrendEnabled() || performanceTrendPanel == null) {
            return false;
        }
        if (forceTrend) {
            return true;
        }
        long now = System.currentTimeMillis();
        long intervalMs = trendSamplingIntervalMs();
        if (lastTrendSampleAtMs <= 0) {
            lastTrendSampleAtMs = now;
            return false;
        }
        if (now - lastTrendSampleAtMs < intervalMs) {
            return false;
        }
        lastTrendSampleAtMs = now;
        return true;
    }

    private void updateTrendView(RemoteProgressSnapshot progress) {
        long now = System.currentTimeMillis();
        PerformanceTrendSnapshot trendSnapshot = trendWindowSampler.sample(
                progress.activeUsers(),
                progress.activeWebSocketConnections(),
                progress.activeSseStreams(),
                progress.totalRequests(),
                progress.failedRequests(),
                progress.report(),
                now
        );
        SwingUtilities.invokeLater(() -> performanceTrendPanel.addOrUpdate(
                new Millisecond(new Date(now)),
                trendSnapshot
        ));
    }

    private void updateFinalTrend(PerformanceJsonReport report, int totalUsers, int workerCount) {
        updateLiveViews(finalProgress(report, totalUsers, workerCount), true);
    }

    private RemoteProgressSnapshot finalProgress(PerformanceJsonReport report, int totalUsers, int workerCount) {
        PerformanceJsonReportSummary summary = report == null ? null : report.getSummary();
        long totalRequests = summary == null ? 0L : summary.getTotalRequests();
        long successRequests = summary == null ? 0L : summary.getSuccessRequests();
        return new RemoteProgressSnapshot(
                0,
                Math.max(0, totalUsers),
                0,
                0,
                totalRequests,
                Math.max(0L, totalRequests - successRequests),
                Math.max(0, workerCount),
                report
        );
    }

    private int totalAssignedUsers(List<PerformanceWorkerAssignment> assignments) {
        if (assignments == null) {
            return 0;
        }
        return assignments.stream()
                .flatMap(assignment -> assignment.getThreadGroups().stream())
                .mapToInt(com.laker.postman.performance.core.worker.PerformanceWorkerThreadGroupAssignment::getVirtualUserCount)
                .sum();
    }

    private boolean isTrendEnabled() {
        return trendEnabledSupplier == null || trendEnabledSupplier.getAsBoolean();
    }

    private boolean isReportRealtimeEnabled() {
        return reportRealtimeEnabledSupplier != null && reportRealtimeEnabledSupplier.getAsBoolean();
    }

    boolean shouldIncludeStatusReport() {
        // WS/SSE 趋势依赖 worker 的协议聚合计数；只拉轻量 status 时只能得到 HTTP 总请求数。
        return isReportRealtimeEnabled() || isTrendEnabled();
    }

    private long trendSamplingIntervalMs() {
        long value = trendSamplingIntervalMsSupplier == null ? POLL_INTERVAL_MS : trendSamplingIntervalMsSupplier.getAsLong();
        return Math.max(POLL_INTERVAL_MS, value);
    }

    private String endpointLabel(PerformanceWorkerEndpoint endpoint) {
        return endpoint == null ? "" : endpoint.getHost() + ":" + endpoint.getPort();
    }

    private record RemoteProgressSnapshot(int activeUsers,
                                          int totalUsers,
                                          int activeWebSocketConnections,
                                          int activeSseStreams,
                                          long totalRequests,
                                          long failedRequests,
                                          int completedWorkers,
                                          PerformanceJsonReport report) {
        private static RemoteProgressSnapshot empty(int totalUsers) {
            return new RemoteProgressSnapshot(0, totalUsers, 0, 0, 0, 0, 0, null);
        }
    }
}

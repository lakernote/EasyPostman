package com.laker.postman.performance.master;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse;
import com.laker.postman.performance.master.PerformanceWorkerReportCollector.PerformanceWorkerReportResult;

import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PerformanceMasterRunExecutor {
    private final PerformanceWorkerAssignmentPlanner assignmentPlanner;
    private final PerformanceWorkerHttpClient workerClient;
    private final PerformanceWorkerReportCollector reportCollector;

    public PerformanceMasterRunExecutor() {
        this(new PerformanceWorkerAssignmentPlanner(), new PerformanceWorkerHttpClient());
    }

    PerformanceMasterRunExecutor(PerformanceWorkerAssignmentPlanner assignmentPlanner,
                                 PerformanceWorkerHttpClient workerClient) {
        this.assignmentPlanner = assignmentPlanner == null ? new PerformanceWorkerAssignmentPlanner() : assignmentPlanner;
        this.workerClient = workerClient == null ? new PerformanceWorkerHttpClient() : workerClient;
        this.reportCollector = new PerformanceWorkerReportCollector(this.workerClient);
    }

    public PerformanceJsonReport execute(PerformanceMasterOptions options) throws Exception {
        if (options == null || options.getPlanPath() == null) {
            throw new IllegalArgumentException("--plan is required");
        }
        if (!Files.isRegularFile(options.getPlanPath())) {
            throw new IllegalArgumentException("Plan file does not exist: " + options.getPlanPath());
        }
        if (options.getWorkers().isEmpty()) {
            throw new IllegalArgumentException("--workers is required");
        }

        PerformanceRunPlan runPlan = new PerformanceRunPlanJsonStorage().load(options.getPlanPath());
        String runId = "run-" + System.currentTimeMillis();
        long deadline = System.currentTimeMillis() + options.getTimeoutMs();
        List<PerformanceWorkerAssignment> assignments = assignmentPlanner.plan(runPlan, options.getWorkers(), runId);
        List<PerformanceWorkerEndpoint> submittedWorkers = new ArrayList<>();
        try {
            for (int i = 0; i < options.getWorkers().size(); i++) {
                PerformanceWorkerEndpoint endpoint = options.getWorkers().get(i);
                PerformanceWorkerAssignment assignment = assignments.get(i);
                workerClient.submitRun(endpoint, PerformanceWorkerRunRequest.builder()
                        .runId(runId)
                        .plan(runPlan)
                        .assignment(assignment)
                        .build(), timeoutUntil(deadline));
                submittedWorkers.add(endpoint);
            }

            waitForWorkers(options, runId, deadline);
        } catch (Exception ex) {
            stopSubmittedWorkers(submittedWorkers, runId, ex);
            throw ex;
        }

        List<PerformanceJsonReport> reports = new ArrayList<>();
        String status = PerformanceRunStatus.SUCCESS;
        for (PerformanceWorkerEndpoint endpoint : options.getWorkers()) {
            PerformanceWorkerReportResult response = reportCollector.collect(endpoint, runId, timeoutUntil(deadline));
            if (!PerformanceRunStatus.SUCCESS.equals(response.status())) {
                status = PerformanceRunStatus.FAILED;
            }
            if (response.report() != null) {
                reports.add(response.report());
            } else if (response.error() != null && !response.error().isBlank()) {
                reports.add(workerErrorReport(endpoint, runId, response));
            }
        }
        return PerformanceJsonReportSummaryMapper.merge(
                runId,
                "master",
                status,
                options.getPlanPath().toString(),
                reports
        );
    }

    private void waitForWorkers(PerformanceMasterOptions options, String runId, long deadline) throws Exception {
        boolean allDone;
        do {
            allDone = true;
            for (PerformanceWorkerEndpoint endpoint : options.getWorkers()) {
                PerformanceWorkerRunStatusResponse status = workerClient.status(endpoint, runId, false, timeoutUntil(deadline));
                if (!isTerminal(status.getStatus())) {
                    allDone = false;
                }
            }
            if (allDone) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException("Timed out waiting for workers");
            }
            Thread.sleep(options.getPollIntervalMs());
        } while (true);
    }

    private boolean isTerminal(String status) {
        return PerformanceRunStatus.isTerminal(status);
    }

    private PerformanceJsonReport workerErrorReport(PerformanceWorkerEndpoint endpoint,
                                                    String runId,
                                                    PerformanceWorkerReportResult response) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(runId)
                        .source(endpoint.getHost() + ":" + endpoint.getPort())
                        .status(response.status())
                        .error(response.error())
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
    }

    private void stopSubmittedWorkers(List<PerformanceWorkerEndpoint> submittedWorkers,
                                      String runId,
                                      Exception cause) {
        for (PerformanceWorkerEndpoint endpoint : submittedWorkers) {
            try {
                workerClient.stop(endpoint, runId, PerformanceWorkerHttpClient.DEFAULT_REQUEST_TIMEOUT);
            } catch (Exception stopEx) {
                cause.addSuppressed(stopEx);
            }
        }
    }

    private Duration timeoutUntil(long deadline) {
        return Duration.ofMillis(Math.max(1L, deadline - System.currentTimeMillis()));
    }
}

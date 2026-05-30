package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.runtime.PerformanceRunExecutionControl;
import com.laker.postman.performance.runtime.PerformanceRunExecutionResult;
import com.laker.postman.performance.runtime.PerformanceRunPlanExecutor;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class DefaultPerformanceWorkerRunExecutor implements PerformanceWorkerRunExecutor {
    private final PerformanceRunPlanExecutor delegate;

    public DefaultPerformanceWorkerRunExecutor() {
        this(new PerformanceRunPlanExecutor());
    }

    DefaultPerformanceWorkerRunExecutor(PerformanceRunPlanExecutor delegate) {
        this.delegate = delegate == null ? new PerformanceRunPlanExecutor() : delegate;
    }

    @Override
    public PerformanceJsonReport execute(PerformanceWorkerRunRequest request,
                                         PerformanceRunExecutionControl control) throws Exception {
        if (request == null || request.getPlan() == null) {
            throw new IllegalArgumentException("Worker run request requires a plan");
        }
        PerformanceRunExecutionResult result = delegate.execute(
                request.getPlan(),
                "worker:" + request.getRunId(),
                request.getAssignment(),
                new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8),
                control
        );
        return withWorkerMetadata(result.getReport(), request);
    }

    private PerformanceJsonReport withWorkerMetadata(PerformanceJsonReport report, PerformanceWorkerRunRequest request) {
        if (report == null) {
            return null;
        }
        PerformanceJsonReportMetadata metadata = report.getMetadata();
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(request.getRunId())
                        .source(workerSource(request.getAssignment()))
                        .status(metadata.getStatus())
                        .planPath("worker:" + request.getRunId())
                        .startTimeMs(metadata.getStartTimeMs())
                        .endTimeMs(metadata.getEndTimeMs())
                        .elapsedTimeMs(metadata.getElapsedTimeMs())
                        .stopped(metadata.isStopped())
                        .error(metadata.getError())
                        .build())
                .summary(report.getSummary())
                .protocols(report.getProtocols())
                .build();
    }

    private String workerSource(PerformanceWorkerAssignment assignment) {
        if (assignment == null) {
            return "worker";
        }
        PerformanceWorkerEndpoint endpoint = assignment.getEndpoint();
        if (endpoint != null) {
            return endpoint.getHost() + ":" + endpoint.getPort();
        }
        return assignment.getWorkerId().isBlank() ? "worker" : assignment.getWorkerId();
    }
}

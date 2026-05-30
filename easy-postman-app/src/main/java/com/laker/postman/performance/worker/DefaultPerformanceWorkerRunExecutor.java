package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
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
        return result.getReport();
    }
}

package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.runtime.PerformanceCoreResultSink;
import com.laker.postman.performance.core.runtime.PerformanceCoreRunSession;
import com.laker.postman.performance.core.runtime.PerformanceRunError;
import com.laker.postman.performance.core.runtime.PerformanceRunHandle;
import com.laker.postman.performance.core.runtime.PerformanceRunSummary;



import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class PerformanceRunSession {
    private final PerformanceCoreRunSession delegate;

    public PerformanceRunSession(BooleanSupplier runningSupplier,
                                 Consumer<Boolean> runningSetter,
                                 PerformanceExecutionEngine executionEngine) {
        this.delegate = new PerformanceCoreRunSession(
                runningSupplier,
                runningSetter,
                executionEngine == null ? null : new PerformanceCoreRunSession.ExecutionEngine() {
                    @Override
                    public void beginRun(long startTimeMs) {
                        executionEngine.beginRun(startTimeMs);
                    }

                    @Override
                    public void beginRun(long startTimeMs, PerformanceCoreResultSink resultSink) {
                        executionEngine.beginRun(startTimeMs, resultSink);
                    }

                    @Override
                    public int getTotalThreads(PerformanceTestPlan plan) {
                        return executionEngine.getTotalThreads(plan);
                    }

                    @Override
                    public void runTestPlan(PerformanceTestPlan plan, int totalThreads) {
                        executionEngine.runTestPlan(plan, totalThreads);
                    }

                    @Override
                    public void cancelAllNetworkCalls() {
                        executionEngine.cancelAllNetworkCalls();
                    }

                    @Override
                    public void endRun() {
                        executionEngine.endRun();
                    }
                }
        );
    }

    public PerformanceRunHandle start(PerformanceRunRequest request) {
        if (request == null) {
            return new PerformanceRunHandle(null, this::stop);
        }
        PerformanceResultSink resultSink = request.getResultSink() == null
                ? PerformanceResultSink.NOOP
                : request.getResultSink();
        return delegate.start(request.getPlan(), resultSink);
    }

    public void stop() {
        delegate.stop();
    }

    public boolean isRunning() {
        return delegate.isRunning();
    }
}

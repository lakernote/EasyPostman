package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import com.laker.postman.performance.core.runtime.PerformanceCoreExecutionEngine;
import com.laker.postman.performance.core.runtime.PerformanceCorePlanExecutor;
import com.laker.postman.performance.core.runtime.PerformanceCoreResultSink;
import com.laker.postman.performance.core.runtime.PerformanceCoreRunSession;
import com.laker.postman.performance.core.runtime.PerformanceRunListener;
import com.laker.postman.performance.core.runtime.PerformanceTransportRuntime;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Runs core thread groups/controllers/timers and delegates request samples to OkHttp.
 * Script, assertion, extractor and protocol-stage semantics are intentionally supplied by higher-level ports/adapters.
 * This is a transport-only execution engine, not a complete headless load-test runner.
 */
final class OkHttpTransportOnlyExecutionEngine implements PerformanceCoreRunSession.ExecutionEngine {
    private final PerformanceTransportRuntime transportRuntime;
    private final PerformanceCorePlanExecutor<Void> planExecutor;
    private final PerformanceCoreExecutionEngine<Void> delegate;
    private volatile PerformanceCoreResultSink resultSink = PerformanceCoreResultSink.NOOP;

    public OkHttpTransportOnlyExecutionEngine(BooleanSupplier runningSupplier,
                                              PerformanceTransportRuntime transportRuntime) {
        this(runningSupplier, transportRuntime, PerformanceRunListener.NOOP);
    }

    public OkHttpTransportOnlyExecutionEngine(BooleanSupplier runningSupplier,
                                              PerformanceTransportRuntime transportRuntime,
                                              PerformanceRunListener runListener) {
        BooleanSupplier resolvedRunningSupplier = runningSupplier == null ? () -> false : runningSupplier;
        this.transportRuntime = transportRuntime == null ? new OkHttpPerformanceTransportRuntime() : transportRuntime;
        this.planExecutor = new PerformanceCorePlanExecutor<>(
                resolvedRunningSupplier,
                this::executeSampler
        );
        this.delegate = new PerformanceCoreExecutionEngine<>(
                resolvedRunningSupplier,
                this.transportRuntime,
                (groupPlan, iterationCount) -> null,
                planExecutor::executeIteration,
                runListener
        );
    }

    private void executeSampler(PerformanceSampler sampler, Void ignored) {
        if (!(sampler instanceof PerformanceCoreRequestSampler requestSampler)) {
            return;
        }
        PerformanceSampleRecord record = transportRuntime.execute(
                PerformanceOutboundRequest.fromSnapshot(requestSampler.getRequestSnapshot())
        );
        PerformanceCoreResultSink sink = currentResultSink();
        if (sink.acceptsSamples()) {
            sink.onSample(record);
        }
    }

    @Override
    public void beginRun(long startTimeMs) {
        beginRun(startTimeMs, PerformanceCoreResultSink.NOOP);
    }

    @Override
    public void beginRun(long startTimeMs, PerformanceCoreResultSink resultSink) {
        this.resultSink = resultSink == null ? PerformanceCoreResultSink.NOOP : resultSink;
        delegate.beginRun(startTimeMs, this.resultSink);
    }

    @Override
    public int getTotalThreads(PerformanceTestPlan plan) {
        return delegate.getTotalThreads(plan);
    }

    public long estimateTotalRequests(PerformanceTestPlan plan) {
        return delegate.estimateTotalRequests(plan);
    }

    @Override
    public void runTestPlan(PerformanceTestPlan plan, int totalThreads) {
        delegate.runTestPlan(plan, totalThreads);
    }

    @Override
    public void cancelAllNetworkCalls() {
        delegate.cancelAllNetworkCalls();
    }

    @Override
    public void endRun() {
        resultSink = PerformanceCoreResultSink.NOOP;
        delegate.endRun();
    }

    public static void joinThreadGroupThreads(List<Thread> threadGroupThreads, Runnable cancellationAction) {
        PerformanceCoreExecutionEngine.joinThreadGroupThreads(threadGroupThreads, cancellationAction);
    }

    public static int calculateStairsTotalSteps(int startThreads, int endThreads, int step) {
        return PerformanceCoreExecutionEngine.calculateStairsTotalSteps(startThreads, endThreads, step);
    }

    private PerformanceCoreResultSink currentResultSink() {
        PerformanceCoreResultSink current = resultSink;
        return current == null ? PerformanceCoreResultSink.NOOP : current;
    }
}

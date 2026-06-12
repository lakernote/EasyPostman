package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.threadgroup.PerformanceCoreThreadGroupPlanner;
import com.laker.postman.performance.core.threadgroup.PerformanceRequestEstimate;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class PerformanceCoreExecutionEngine<C> implements PerformanceCoreRunSession.ExecutionEngine {
    private final BooleanSupplier runningSupplier;
    private final PerformanceNetworkControl networkControl;
    private final PerformanceCoreThreadGroupPlanner threadGroupPlanner = new PerformanceCoreThreadGroupPlanner();
    private final PerformanceVirtualUserCoordinator virtualUsers;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final PerformanceCoreThreadGroupRunner<C> threadGroupRunner;
    private final PerformanceRunListener runListener;
    private volatile long startTime;
    private volatile PerformanceCoreResultSink resultSink = PerformanceCoreResultSink.NOOP;

    public PerformanceCoreExecutionEngine(BooleanSupplier runningSupplier,
                                          PerformanceNetworkControl networkControl,
                                          PerformanceCoreThreadGroupRunner.IterationContextFactory<C> iterationContextFactory,
                                          PerformanceCoreThreadGroupRunner.IterationExecutor<C> iterationExecutor,
                                          PerformanceRunListener runListener) {
        this(
                runningSupplier,
                networkControl,
                new PerformanceVirtualUserCoordinator(),
                new PerformanceRealtimeMetrics(),
                iterationContextFactory,
                iterationExecutor,
                runListener
        );
    }

    public PerformanceCoreExecutionEngine(BooleanSupplier runningSupplier,
                                          PerformanceNetworkControl networkControl,
                                          PerformanceVirtualUserCoordinator virtualUsers,
                                          PerformanceRealtimeMetrics realtimeMetrics,
                                          PerformanceCoreThreadGroupRunner.IterationContextFactory<C> iterationContextFactory,
                                          PerformanceCoreThreadGroupRunner.IterationExecutor<C> iterationExecutor,
                                          PerformanceRunListener runListener) {
        this.runningSupplier = runningSupplier == null ? () -> false : runningSupplier;
        this.networkControl = networkControl == null ? PerformanceNetworkControl.NOOP : networkControl;
        this.virtualUsers = virtualUsers == null ? new PerformanceVirtualUserCoordinator() : virtualUsers;
        this.realtimeMetrics = realtimeMetrics == null ? new PerformanceRealtimeMetrics() : realtimeMetrics;
        this.runListener = runListener == null ? PerformanceRunListener.NOOP : runListener;
        this.resultSink = compositeResultSink(PerformanceCoreResultSink.NOOP, this.runListener);
        this.threadGroupRunner = new PerformanceCoreThreadGroupRunner<>(
                this.runningSupplier,
                () -> this.startTime,
                this::cancelAllNetworkCalls,
                this.virtualUsers,
                iterationContextFactory,
                iterationExecutor,
                this::currentResultSink
        );
    }

    public boolean isRunning() {
        return runningSupplier.getAsBoolean();
    }

    public int getActiveThreads() {
        return virtualUsers.getActiveThreads();
    }

    public int sampleWindowPeakActiveThreads() {
        return virtualUsers.sampleWindowPeakActiveThreads();
    }

    public int getActiveWebSockets() {
        return networkControl.activeWebSocketCount();
    }

    public int getActiveSseStreams() {
        return networkControl.activeSseCount();
    }

    @Override
    public void beginRun(long startTimeMs) {
        beginRun(startTimeMs, PerformanceCoreResultSink.NOOP);
    }

    @Override
    public void beginRun(long startTimeMs, PerformanceCoreResultSink resultSink) {
        this.startTime = startTimeMs;
        realtimeMetrics.reset(startTimeMs);
        this.resultSink = compositeResultSink(resultSink, runListener);
    }

    public long getStartTime() {
        return startTime;
    }

    public void resetVirtualUsers() {
        virtualUsers.resetVirtualUsers();
    }

    public PerformanceRealtimeMetrics getRealtimeMetrics() {
        return realtimeMetrics;
    }

    public PerformanceRealtimeMetrics.Sample drainRealtimeMetricsWindow(long nowMs) {
        return realtimeMetrics.drainWindow(nowMs);
    }

    public PerformanceRealtimeMetrics.LiveSnapshot liveRealtimeMetrics(long nowMs) {
        return realtimeMetrics.liveSnapshot(nowMs);
    }

    @Override
    public int getTotalThreads(PerformanceTestPlan plan) {
        return threadGroupPlanner.getTotalThreads(plan);
    }

    public long estimateTotalRequests(PerformanceTestPlan plan) {
        return threadGroupPlanner.estimateTotalRequests(plan);
    }

    public PerformanceRequestEstimate estimateRequestCount(PerformanceTestPlan plan) {
        return threadGroupPlanner.estimateRequestCount(plan);
    }

    @Override
    public void runTestPlan(PerformanceTestPlan plan, int totalThreads) {
        if (!runningSupplier.getAsBoolean()) {
            return;
        }
        threadGroupRunner.run(plan, totalThreads);
    }

    @Override
    public void cancelAllNetworkCalls() {
        networkControl.cancelAll();
    }

    public static void joinThreadGroupThreads(List<Thread> threadGroupThreads, Runnable cancellationAction) {
        PerformanceCoreThreadGroupRunner.joinThreadGroupThreads(threadGroupThreads, cancellationAction);
    }

    public static int calculateStairsTotalSteps(int startThreads, int endThreads, int step) {
        return PerformanceCoreThreadGroupRunner.calculateStairsTotalSteps(startThreads, endThreads, step);
    }

    private PerformanceCoreResultSink currentResultSink() {
        PerformanceCoreResultSink current = resultSink;
        return current == null ? PerformanceCoreResultSink.NOOP : current;
    }

    private static PerformanceCoreResultSink compositeResultSink(PerformanceCoreResultSink resultSink,
                                                                PerformanceRunListener runListener) {
        PerformanceCoreResultSink resolvedSink = resultSink == null ? PerformanceCoreResultSink.NOOP : resultSink;
        PerformanceRunListener resolvedListener = runListener == null ? PerformanceRunListener.NOOP : runListener;
        return new PerformanceCoreResultSink() {
            @Override
            public void onSample(com.laker.postman.performance.core.model.PerformanceSampleRecord record) {
                resolvedSink.onSample(record);
            }

            @Override
            public boolean acceptsSamples() {
                return resolvedSink.acceptsSamples();
            }

            @Override
            public void onProgress(PerformanceRunProgress progress) {
                resolvedSink.onProgress(progress);
                resolvedListener.onProgress(progress);
            }

            @Override
            public void onError(PerformanceRunError error) {
                resolvedSink.onError(error);
                resolvedListener.onError(error);
            }

            @Override
            public void onComplete(PerformanceRunSummary summary) {
                resolvedSink.onComplete(summary);
            }
        };
    }

    @Override
    public void endRun() {
        resultSink = compositeResultSink(PerformanceCoreResultSink.NOOP, runListener);
    }
}

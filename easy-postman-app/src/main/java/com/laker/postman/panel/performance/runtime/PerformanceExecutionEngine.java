package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.runtime.PerformanceCoreExecutionEngine;
import com.laker.postman.performance.core.runtime.PerformanceCoreResultSink;
import com.laker.postman.performance.core.runtime.PerformanceNetworkControl;
import com.laker.postman.performance.core.runtime.PerformanceRunListener;
import com.laker.postman.performance.core.runtime.PerformanceVirtualUserCoordinator;


import com.laker.postman.panel.performance.execution.DefaultPerformanceNetworkRuntime;
import com.laker.postman.panel.performance.execution.PerformanceExecutionConfig;
import com.laker.postman.panel.performance.execution.PerformanceNetworkRuntime;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import com.laker.postman.service.variable.ExecutionVariableContext;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class PerformanceExecutionEngine {

    private final PerformanceNetworkRuntime networkRuntime;
    private final PerformanceCoreExecutionEngine<ExecutionVariableContext> delegate;
    private volatile PerformanceCoreResultSink resultSink = PerformanceCoreResultSink.NOOP;

    public PerformanceExecutionEngine(BooleanSupplier runningSupplier,
                                      BooleanSupplier efficientModeSupplier,
                                      IntSupplier responseBodyPreviewLimitKbSupplier,
                                      PerformanceResultCollector resultCollector) {
        this(runningSupplier,
                PerformanceExecutionConfig.supplying(efficientModeSupplier, responseBodyPreviewLimitKbSupplier, () -> false),
                resultCollector,
                PerformanceRunListener.NOOP);
    }

    public PerformanceExecutionEngine(BooleanSupplier runningSupplier,
                                      BooleanSupplier efficientModeSupplier,
                                      IntSupplier responseBodyPreviewLimitKbSupplier,
                                      PerformanceResultCollector resultCollector,
                                      PerformanceRunListener runListener) {
        this(runningSupplier,
                PerformanceExecutionConfig.supplying(efficientModeSupplier, responseBodyPreviewLimitKbSupplier, () -> false),
                resultCollector,
                runListener);
    }

    public PerformanceExecutionEngine(BooleanSupplier runningSupplier,
                                      BooleanSupplier efficientModeSupplier,
                                      IntSupplier responseBodyPreviewLimitKbSupplier,
                                      PerformanceResultCollector resultCollector,
                                      PerformanceRunListener runListener,
                                      BooleanSupplier eventLoggingEnabledSupplier) {
        this(
                runningSupplier,
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier,
                resultCollector,
                runListener,
                eventLoggingEnabledSupplier,
                new DefaultPerformanceNetworkRuntime()
        );
    }

    public PerformanceExecutionEngine(BooleanSupplier runningSupplier,
                                      BooleanSupplier efficientModeSupplier,
                                      IntSupplier responseBodyPreviewLimitKbSupplier,
                                      PerformanceResultCollector resultCollector,
                                      PerformanceRunListener runListener,
                                      BooleanSupplier eventLoggingEnabledSupplier,
                                      PerformanceNetworkRuntime networkRuntime) {
        this(runningSupplier,
                PerformanceExecutionConfig.supplying(
                        efficientModeSupplier,
                        responseBodyPreviewLimitKbSupplier,
                        eventLoggingEnabledSupplier
                ),
                resultCollector,
                runListener,
                networkRuntime);
    }

    public PerformanceExecutionEngine(BooleanSupplier runningSupplier,
                                      PerformanceExecutionConfig executionConfig,
                                      PerformanceResultCollector resultCollector) {
        this(runningSupplier, executionConfig, resultCollector, PerformanceRunListener.NOOP);
    }

    public PerformanceExecutionEngine(BooleanSupplier runningSupplier,
                                      PerformanceExecutionConfig executionConfig,
                                      PerformanceResultCollector resultCollector,
                                      PerformanceRunListener runListener) {
        this(runningSupplier, executionConfig, resultCollector, runListener, new DefaultPerformanceNetworkRuntime());
    }

    public PerformanceExecutionEngine(BooleanSupplier runningSupplier,
                                      PerformanceExecutionConfig executionConfig,
                                      PerformanceResultCollector resultCollector,
                                      PerformanceRunListener runListener,
                                      PerformanceNetworkRuntime networkRuntime) {
        this.networkRuntime = networkRuntime == null ? new DefaultPerformanceNetworkRuntime() : networkRuntime;
        PerformanceExecutionConfig resolvedConfig = executionConfig == null
                ? PerformanceExecutionConfig.DEFAULT
                : executionConfig;
        PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
        PerformanceRealtimeMetrics realtimeMetrics = new PerformanceRealtimeMetrics();
        PerformanceRequestExecutor requestExecutor = new PerformanceRequestExecutor(
                runningSupplier,
                this::isCancelledOrInterrupted,
                this.networkRuntime.activeSseSources(),
                this.networkRuntime.activeWebSockets(),
                realtimeMetrics,
                resolvedConfig,
                this.networkRuntime
        );
        PerformanceSamplerExecutor samplerExecutor = new PerformanceSamplerExecutor(
                runningSupplier,
                resolvedConfig::isEfficientMode,
                requestExecutor,
                resultCollector,
                this::currentResultSink
        );
        PerformanceIterationContextFactory iterationContextFactory = new PerformanceIterationContextFactory(virtualUsers);
        PerformancePlanExecutor planExecutor = new PerformancePlanExecutor(runningSupplier, samplerExecutor);
        this.delegate = new PerformanceCoreExecutionEngine<>(
                runningSupplier,
                new PerformanceNetworkControl() {
                    @Override
                    public int activeWebSocketCount() {
                        return PerformanceExecutionEngine.this.networkRuntime.activeWebSocketCount();
                    }

                    @Override
                    public int activeSseCount() {
                        return PerformanceExecutionEngine.this.networkRuntime.activeSseCount();
                    }

                    @Override
                    public void cancelAll() {
                        PerformanceExecutionEngine.this.networkRuntime.cancelAll();
                    }
                },
                virtualUsers,
                realtimeMetrics,
                iterationContextFactory::create,
                planExecutor::executeIteration,
                runListener
        );
    }

    public int getActiveThreads() {
        return delegate.getActiveThreads();
    }

    public int getActiveWebSockets() {
        return delegate.getActiveWebSockets();
    }

    public int getActiveSseStreams() {
        return delegate.getActiveSseStreams();
    }

    public void beginRun(long startTime) {
        beginRun(startTime, PerformanceCoreResultSink.NOOP);
    }

    public void beginRun(long startTime, PerformanceCoreResultSink resultSink) {
        this.resultSink = resultSink == null ? PerformanceCoreResultSink.NOOP : resultSink;
        delegate.beginRun(startTime, this.resultSink);
    }

    public long getStartTime() {
        return delegate.getStartTime();
    }

    public void resetVirtualUsers() {
        delegate.resetVirtualUsers();
    }

    public PerformanceRealtimeMetrics.Sample sampleRealtimeMetrics(long nowMs) {
        return delegate.sampleRealtimeMetrics(nowMs);
    }

    public PerformanceRealtimeMetrics.LiveSnapshot liveRealtimeMetrics(long nowMs) {
        return delegate.liveRealtimeMetrics(nowMs);
    }

    public int getTotalThreads(PerformanceTestPlan plan) {
        return delegate.getTotalThreads(plan);
    }

    public long estimateTotalRequests(PerformanceTestPlan plan) {
        return delegate.estimateTotalRequests(plan);
    }

    public void runTestPlan(PerformanceTestPlan plan, int totalThreads) {
        delegate.runTestPlan(plan, totalThreads);
    }

    public void cancelAllNetworkCalls() {
        delegate.cancelAllNetworkCalls();
    }

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

    private boolean isCancelledOrInterrupted(Throwable ex) {
        if (ex == null) {
            return false;
        }
        if (ex instanceof java.io.InterruptedIOException) {
            return true;
        }
        if (ex instanceof java.io.IOException ioException) {
            String message = ioException.getMessage();
            if (message != null && message.contains("Canceled")) {
                return true;
            }
        }
        if (ex instanceof InterruptedException) {
            return true;
        }
        return isCancelledOrInterrupted(ex.getCause());
    }

    private PerformanceCoreResultSink currentResultSink() {
        PerformanceCoreResultSink current = resultSink;
        return current == null ? PerformanceCoreResultSink.NOOP : current;
    }
}

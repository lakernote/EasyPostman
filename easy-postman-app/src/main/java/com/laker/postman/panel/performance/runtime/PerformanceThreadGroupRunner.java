package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.runtime.*;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.service.variable.ExecutionVariableContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public final class PerformanceThreadGroupRunner {

    private final PerformanceCoreThreadGroupRunner<ExecutionVariableContext> delegate;

    public PerformanceThreadGroupRunner(BooleanSupplier runningSupplier,
                                        LongSupplier startTimeSupplier,
                                        Runnable cancellationAction,
                                        PerformanceVirtualUserCoordinator virtualUsers,
                                        PerformanceIterationContextFactory iterationContextFactory,
                                        PerformancePlanExecutor planExecutor,
                                        PerformanceRunListener runListener) {
        this.delegate = new PerformanceCoreThreadGroupRunner<>(
                runningSupplier,
                startTimeSupplier,
                cancellationAction,
                virtualUsers,
                iterationContextFactory == null ? null : iterationContextFactory::create,
                planExecutor == null ? null : planExecutor::executeIteration,
                () -> listenerSink(runListener)
        );
    }

    public void run(PerformanceTestPlan plan, int totalThreads) {
        delegate.run(plan, totalThreads);
    }

    public void adjustSpikeThreadCount(PerformanceThreadGroupPlan groupPlan,
                                       ThreadGroupData tg,
                                       AtomicInteger activeWorkerThreads,
                                       int targetThreads,
                                       int totalTime,
                                       BiConsumer<Integer, Integer> progressUpdater,
                                       int totalThreads,
                                       ConcurrentHashMap<Thread, Long> threadEndTimes) {
        delegate.adjustSpikeThreadCount(
                groupPlan,
                tg,
                activeWorkerThreads,
                targetThreads,
                totalTime,
                progressUpdater,
                totalThreads,
                threadEndTimes
        );
    }

    private static PerformanceCoreResultSink listenerSink(PerformanceRunListener runListener) {
        PerformanceRunListener resolvedListener = runListener == null ? PerformanceRunListener.NOOP : runListener;
        return new PerformanceCoreResultSink() {
            @Override
            public void onProgress(PerformanceRunProgress progress) {
                resolvedListener.onProgress(progress);
            }

            @Override
            public void onError(PerformanceRunError error) {
                resolvedListener.onError(error);
            }
        };
    }
}

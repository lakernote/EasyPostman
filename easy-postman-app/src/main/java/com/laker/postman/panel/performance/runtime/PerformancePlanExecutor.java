package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.runtime.PerformanceCorePlanExecutor;


import com.laker.postman.service.variable.ExecutionVariableContext;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class PerformancePlanExecutor {

    @FunctionalInterface
    public interface TimerSleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    private final PerformanceCorePlanExecutor<ExecutionVariableContext> delegate;

    public PerformancePlanExecutor(BooleanSupplier runningSupplier,
                                   PerformanceSamplerExecutor samplerExecutor) {
        this(runningSupplier, samplerExecutor, delayMs -> TimeUnit.MILLISECONDS.sleep(delayMs));
    }

    public PerformancePlanExecutor(BooleanSupplier runningSupplier,
                                   PerformanceSamplerExecutor samplerExecutor,
                                   TimerSleeper timerSleeper) {
        this.delegate = new PerformanceCorePlanExecutor<>(
                runningSupplier,
                samplerExecutor::execute,
                timerSleeper::sleep
        );
    }

    public void executeIteration(PerformanceThreadGroupPlan groupPlan,
                                 ExecutionVariableContext iterationContext) {
        delegate.executeIteration(groupPlan, iterationContext);
    }
}

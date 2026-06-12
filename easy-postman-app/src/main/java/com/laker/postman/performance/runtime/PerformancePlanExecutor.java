package com.laker.postman.performance.runtime;

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
        this(runningSupplier, samplerExecutor, TimeUnit.MILLISECONDS::sleep);
    }

    public PerformancePlanExecutor(BooleanSupplier runningSupplier,
                                   PerformanceSamplerExecutor samplerExecutor,
                                   TimerSleeper timerSleeper) {
        this.delegate = new PerformanceCorePlanExecutor<>(
                runningSupplier,
                samplerExecutor::execute,
                timerSleeper::sleep,
                PerformanceConditionEvaluator::evaluate,
                (controller, context) -> context != null && context.markOnceOnlyExecuted(
                        "once-only:" + System.identityHashCode(controller)
                ),
                PerformanceConditionEvaluator::evaluate
        );
    }

    public void executeIteration(PerformanceThreadGroupPlan groupPlan,
                                 ExecutionVariableContext iterationContext) {
        delegate.executeIteration(groupPlan, iterationContext);
    }
}

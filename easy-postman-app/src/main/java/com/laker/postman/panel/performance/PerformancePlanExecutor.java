package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.plan.PerformanceLoopController;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;
import com.laker.postman.panel.performance.plan.PerformanceTimerElement;
import com.laker.postman.panel.performance.timer.TimerData;
import com.laker.postman.service.variable.ExecutionVariableContext;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

final class PerformancePlanExecutor {

    @FunctionalInterface
    interface TimerSleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    private final BooleanSupplier runningSupplier;
    private final PerformanceSamplerExecutor samplerExecutor;
    private final TimerSleeper timerSleeper;

    PerformancePlanExecutor(BooleanSupplier runningSupplier,
                            PerformanceSamplerExecutor samplerExecutor) {
        this(runningSupplier, samplerExecutor, delayMs -> TimeUnit.MILLISECONDS.sleep(delayMs));
    }

    PerformancePlanExecutor(BooleanSupplier runningSupplier,
                            PerformanceSamplerExecutor samplerExecutor,
                            TimerSleeper timerSleeper) {
        this.runningSupplier = runningSupplier;
        this.samplerExecutor = samplerExecutor;
        this.timerSleeper = timerSleeper;
    }

    void executeIteration(PerformanceThreadGroupPlan groupPlan,
                          ExecutionVariableContext iterationContext) {
        if (groupPlan == null) {
            return;
        }
        executeElements(groupPlan.getElements(), iterationContext);
    }

    private void executeElements(List<PerformancePlanElement> elements,
                                 ExecutionVariableContext iterationContext) {
        for (PerformancePlanElement element : elements) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            executeElement(element, iterationContext);
        }
    }

    private void executeElement(PerformancePlanElement element,
                                ExecutionVariableContext iterationContext) {
        if (element instanceof PerformanceLoopController loopController) {
            executeLoop(loopController, iterationContext);
            return;
        }
        if (element instanceof PerformanceTimerElement timerElement) {
            sleepTimer(timerElement);
            return;
        }
        if (element instanceof PerformanceRequestSampler requestSampler) {
            executeRequestSampler(requestSampler, iterationContext);
        }
    }

    private void executeLoop(PerformanceLoopController loopController,
                             ExecutionVariableContext iterationContext) {
        int iterations = loopController.getLoopData() == null ? 1 : loopController.getLoopData().iterations;
        for (int iteration = 0; iteration < iterations && runningSupplier.getAsBoolean(); iteration++) {
            executeElements(loopController.getElements(), iterationContext);
        }
    }

    private void executeRequestSampler(PerformanceRequestSampler requestSampler,
                                       ExecutionVariableContext iterationContext) {
        PerformanceRequestExecutionResult executionResult = samplerExecutor.execute(requestSampler, iterationContext);
        if (executionResult == null || !runningSupplier.getAsBoolean() || executionResult.webSocketRequest) {
            return;
        }
        for (PerformancePlanElement child : requestSampler.getChildren()) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            if (child instanceof PerformanceTimerElement timerElement) {
                sleepTimer(timerElement);
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
        }
    }

    private void sleepTimer(PerformanceTimerElement timerElement) {
        TimerData timerData = timerElement.getTimerData();
        if (timerData == null) {
            return;
        }
        try {
            timerSleeper.sleep(timerData.delayMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

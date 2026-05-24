package com.laker.postman.panel.performance.runtime;

import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.plan.PerformanceController;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceSampler;
import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;
import com.laker.postman.panel.performance.plan.PerformanceTimerElement;
import com.laker.postman.panel.performance.timer.TimerData;
import com.laker.postman.service.variable.ExecutionVariableContext;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class PerformancePlanExecutor {

    @FunctionalInterface
    public interface TimerSleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    private final BooleanSupplier runningSupplier;
    private final PerformanceSamplerExecutor samplerExecutor;
    private final TimerSleeper timerSleeper;

    public PerformancePlanExecutor(BooleanSupplier runningSupplier,
                                   PerformanceSamplerExecutor samplerExecutor) {
        this(runningSupplier, samplerExecutor, delayMs -> TimeUnit.MILLISECONDS.sleep(delayMs));
    }

    public PerformancePlanExecutor(BooleanSupplier runningSupplier,
                                   PerformanceSamplerExecutor samplerExecutor,
                                   TimerSleeper timerSleeper) {
        this.runningSupplier = runningSupplier;
        this.samplerExecutor = samplerExecutor;
        this.timerSleeper = timerSleeper;
    }

    public void executeIteration(PerformanceThreadGroupPlan groupPlan,
                                 ExecutionVariableContext iterationContext) {
        if (groupPlan == null) {
            return;
        }
        executeElements(groupPlan.getElements(), PerformanceTimerScope.empty(), iterationContext);
    }

    private void executeElements(List<PerformancePlanElement> elements,
                                 PerformanceTimerScope inheritedScope,
                                 ExecutionVariableContext iterationContext) {
        PerformanceTimerScope scopedTimers = inheritedScope.enter(elements);
        for (PerformancePlanElement element : elements) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            executeElement(element, scopedTimers, iterationContext);
        }
    }

    private void executeElement(PerformancePlanElement element,
                                PerformanceTimerScope scopedTimers,
                                ExecutionVariableContext iterationContext) {
        if (element instanceof PerformanceController controller) {
            executeController(controller, scopedTimers, iterationContext);
            return;
        }
        if (element instanceof PerformanceTimerElement) {
            // Timers are scoped to samplers in the current controller, not standalone executable steps.
            return;
        }
        if (element instanceof PerformanceSampler sampler) {
            executeSampler(sampler, scopedTimers, iterationContext);
        }
    }

    private void executeController(PerformanceController controller,
                                   PerformanceTimerScope scopedTimers,
                                   ExecutionVariableContext iterationContext) {
        int iterations = controller.getIterationCount();
        for (int iteration = 0; iteration < iterations && runningSupplier.getAsBoolean(); iteration++) {
            executeElements(controller.getElements(), scopedTimers, iterationContext);
        }
    }

    private void executeSampler(PerformanceSampler sampler,
                                PerformanceTimerScope scopedTimers,
                                ExecutionVariableContext iterationContext) {
        sleepTimers(scopedTimers.timersForSampler(sampler));
        if (Thread.currentThread().isInterrupted() || !runningSupplier.getAsBoolean()) {
            return;
        }
        PerformanceRequestExecutionResult executionResult = samplerExecutor.execute(sampler, iterationContext);
        if (executionResult == null || !runningSupplier.getAsBoolean()) {
            return;
        }
    }

    private void sleepTimers(List<PerformanceTimerElement> timerElements) {
        for (PerformanceTimerElement timerElement : timerElements) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            sleepTimer(timerElement);
            if (Thread.currentThread().isInterrupted()) {
                return;
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

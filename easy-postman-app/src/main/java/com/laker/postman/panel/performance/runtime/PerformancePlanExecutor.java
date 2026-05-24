package com.laker.postman.panel.performance.runtime;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.plan.PerformanceLoopController;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;
import com.laker.postman.panel.performance.plan.PerformanceTimerElement;
import com.laker.postman.panel.performance.timer.TimerData;
import com.laker.postman.service.variable.ExecutionVariableContext;

import java.util.ArrayList;
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
        executeElements(groupPlan.getElements(), List.of(), iterationContext);
    }

    private void executeElements(List<PerformancePlanElement> elements,
                                 List<PerformanceTimerElement> inheritedTimers,
                                 ExecutionVariableContext iterationContext) {
        List<PerformanceTimerElement> scopedTimers = mergeTimers(inheritedTimers, collectDirectTimers(elements));
        for (PerformancePlanElement element : elements) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            executeElement(element, scopedTimers, iterationContext);
        }
    }

    private void executeElement(PerformancePlanElement element,
                                List<PerformanceTimerElement> scopedTimers,
                                ExecutionVariableContext iterationContext) {
        if (element instanceof PerformanceLoopController loopController) {
            executeLoop(loopController, scopedTimers, iterationContext);
            return;
        }
        if (element instanceof PerformanceTimerElement) {
            // Timers are scoped to samplers in the current controller, not standalone executable steps.
            return;
        }
        if (element instanceof PerformanceRequestSampler requestSampler) {
            executeRequestSampler(requestSampler, scopedTimers, iterationContext);
        }
    }

    private void executeLoop(PerformanceLoopController loopController,
                             List<PerformanceTimerElement> scopedTimers,
                             ExecutionVariableContext iterationContext) {
        int iterations = loopController.getLoopData() == null ? 1 : loopController.getLoopData().iterations;
        for (int iteration = 0; iteration < iterations && runningSupplier.getAsBoolean(); iteration++) {
            executeElements(loopController.getElements(), scopedTimers, iterationContext);
        }
    }

    private void executeRequestSampler(PerformanceRequestSampler requestSampler,
                                       List<PerformanceTimerElement> scopedTimers,
                                       ExecutionVariableContext iterationContext) {
        sleepTimers(timersForSampler(requestSampler, scopedTimers));
        if (Thread.currentThread().isInterrupted() || !runningSupplier.getAsBoolean()) {
            return;
        }
        PerformanceRequestExecutionResult executionResult = samplerExecutor.execute(requestSampler, iterationContext);
        if (executionResult == null || !runningSupplier.getAsBoolean()) {
            return;
        }
    }

    private List<PerformanceTimerElement> timersForSampler(PerformanceRequestSampler requestSampler,
                                                           List<PerformanceTimerElement> scopedTimers) {
        if (isWebSocketSampler(requestSampler)) {
            return scopedTimers;
        }
        return mergeTimers(scopedTimers, collectDirectTimers(requestSampler.getChildren()));
    }

    private boolean isWebSocketSampler(PerformanceRequestSampler requestSampler) {
        if (requestSampler == null) {
            return false;
        }
        HttpRequestItem requestItem = requestSampler.getHttpRequestItem();
        return requestItem != null
                && requestItem.getProtocol() != null
                && requestItem.getProtocol().isWebSocketProtocol();
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

    private List<PerformanceTimerElement> collectDirectTimers(List<PerformancePlanElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }
        List<PerformanceTimerElement> timers = new ArrayList<>();
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceTimerElement timerElement) {
                timers.add(timerElement);
            }
        }
        return timers;
    }

    private List<PerformanceTimerElement> mergeTimers(List<PerformanceTimerElement> first,
                                                      List<PerformanceTimerElement> second) {
        if ((first == null || first.isEmpty()) && (second == null || second.isEmpty())) {
            return List.of();
        }
        List<PerformanceTimerElement> merged = new ArrayList<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return List.copyOf(merged);
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

package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.plan.PerformanceTimerElement;
import com.laker.postman.performance.core.timer.TimerData;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class PerformanceCorePlanExecutor<C> {

    @FunctionalInterface
    public interface SamplerExecutor<C> {
        void execute(PerformanceSampler sampler, C iterationContext);
    }

    @FunctionalInterface
    public interface TimerSleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    private final BooleanSupplier runningSupplier;
    private final SamplerExecutor<C> samplerExecutor;
    private final TimerSleeper timerSleeper;

    public PerformanceCorePlanExecutor(BooleanSupplier runningSupplier,
                                       SamplerExecutor<C> samplerExecutor) {
        this(runningSupplier, samplerExecutor, delayMs -> TimeUnit.MILLISECONDS.sleep(delayMs));
    }

    public PerformanceCorePlanExecutor(BooleanSupplier runningSupplier,
                                       SamplerExecutor<C> samplerExecutor,
                                       TimerSleeper timerSleeper) {
        this.runningSupplier = Objects.requireNonNull(runningSupplier, "runningSupplier");
        this.samplerExecutor = Objects.requireNonNull(samplerExecutor, "samplerExecutor");
        this.timerSleeper = Objects.requireNonNull(timerSleeper, "timerSleeper");
    }

    public void executeIteration(PerformanceThreadGroupPlan groupPlan, C iterationContext) {
        if (groupPlan == null) {
            return;
        }
        executeElements(groupPlan.getElements(), PerformanceCoreTimerScope.empty(), iterationContext);
    }

    private void executeElements(List<PerformancePlanElement> elements,
                                 PerformanceCoreTimerScope inheritedScope,
                                 C iterationContext) {
        PerformanceCoreTimerScope scopedTimers = inheritedScope.enter(elements);
        for (PerformancePlanElement element : elements) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            executeElement(element, scopedTimers, iterationContext);
        }
    }

    private void executeElement(PerformancePlanElement element,
                                PerformanceCoreTimerScope scopedTimers,
                                C iterationContext) {
        if (element instanceof PerformanceController controller) {
            executeController(controller, scopedTimers, iterationContext);
            return;
        }
        if (element instanceof PerformanceTimerElement) {
            return;
        }
        if (element instanceof PerformanceSampler sampler) {
            executeSampler(sampler, scopedTimers, iterationContext);
        }
    }

    private void executeController(PerformanceController controller,
                                   PerformanceCoreTimerScope scopedTimers,
                                   C iterationContext) {
        int iterations = controller.getIterationCount();
        for (int iteration = 0; iteration < iterations && runningSupplier.getAsBoolean(); iteration++) {
            executeElements(controller.getElements(), scopedTimers, iterationContext);
        }
    }

    private void executeSampler(PerformanceSampler sampler,
                                PerformanceCoreTimerScope scopedTimers,
                                C iterationContext) {
        sleepTimers(scopedTimers.timersForSampler(sampler));
        if (Thread.currentThread().isInterrupted() || !runningSupplier.getAsBoolean()) {
            return;
        }
        samplerExecutor.execute(sampler, iterationContext);
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

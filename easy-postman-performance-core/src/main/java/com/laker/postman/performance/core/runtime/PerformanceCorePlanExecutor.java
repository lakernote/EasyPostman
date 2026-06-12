package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.controller.ConditionExpressionEvaluator;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.plan.PerformanceConditionController;
import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformanceOnceOnlyController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceWhileController;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.plan.PerformanceTimerElement;
import com.laker.postman.performance.core.timer.TimerData;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    @FunctionalInterface
    public interface ConditionEvaluator<C> {
        boolean evaluate(PerformanceConditionController conditionController, C iterationContext);
    }

    @FunctionalInterface
    public interface OnceOnlyState<C> {
        boolean enter(PerformanceOnceOnlyController onceOnlyController, C iterationContext);
    }

    @FunctionalInterface
    public interface WhileEvaluator<C> {
        boolean evaluate(PerformanceWhileController whileController, C iterationContext);
    }

    private final BooleanSupplier runningSupplier;
    private final SamplerExecutor<C> samplerExecutor;
    private final TimerSleeper timerSleeper;
    private final ConditionEvaluator<C> conditionEvaluator;
    private final OnceOnlyState<C> onceOnlyState;
    private final WhileEvaluator<C> whileEvaluator;
    private final DefaultOnceOnlyState<C> defaultOnceOnlyState;

    public PerformanceCorePlanExecutor(BooleanSupplier runningSupplier,
                                       SamplerExecutor<C> samplerExecutor) {
        this(runningSupplier, samplerExecutor, delayMs -> TimeUnit.MILLISECONDS.sleep(delayMs));
    }

    public PerformanceCorePlanExecutor(BooleanSupplier runningSupplier,
                                       SamplerExecutor<C> samplerExecutor,
                                       TimerSleeper timerSleeper) {
        this(runningSupplier, samplerExecutor, timerSleeper, defaultConditionEvaluator());
    }

    public PerformanceCorePlanExecutor(BooleanSupplier runningSupplier,
                                       SamplerExecutor<C> samplerExecutor,
                                       TimerSleeper timerSleeper,
                                       ConditionEvaluator<C> conditionEvaluator) {
        this(runningSupplier, samplerExecutor, timerSleeper, conditionEvaluator, new DefaultOnceOnlyState<>());
    }

    public PerformanceCorePlanExecutor(BooleanSupplier runningSupplier,
                                       SamplerExecutor<C> samplerExecutor,
                                       TimerSleeper timerSleeper,
                                       ConditionEvaluator<C> conditionEvaluator,
                                       OnceOnlyState<C> onceOnlyState) {
        this(runningSupplier, samplerExecutor, timerSleeper, conditionEvaluator, onceOnlyState, defaultWhileEvaluator());
    }

    public PerformanceCorePlanExecutor(BooleanSupplier runningSupplier,
                                       SamplerExecutor<C> samplerExecutor,
                                       TimerSleeper timerSleeper,
                                       ConditionEvaluator<C> conditionEvaluator,
                                       OnceOnlyState<C> onceOnlyState,
                                       WhileEvaluator<C> whileEvaluator) {
        this.runningSupplier = Objects.requireNonNull(runningSupplier, "runningSupplier");
        this.samplerExecutor = Objects.requireNonNull(samplerExecutor, "samplerExecutor");
        this.timerSleeper = Objects.requireNonNull(timerSleeper, "timerSleeper");
        this.conditionEvaluator = Objects.requireNonNull(conditionEvaluator, "conditionEvaluator");
        this.onceOnlyState = Objects.requireNonNull(onceOnlyState, "onceOnlyState");
        this.whileEvaluator = Objects.requireNonNull(whileEvaluator, "whileEvaluator");
        this.defaultOnceOnlyState = asDefaultOnceOnlyState(onceOnlyState);
    }

    public void executeIteration(PerformanceThreadGroupPlan groupPlan, C iterationContext) {
        if (groupPlan == null) {
            return;
        }
        if (defaultOnceOnlyState == null) {
            executeElements(groupPlan.getElements(), PerformanceCoreTimerScope.empty(), iterationContext);
            return;
        }
        defaultOnceOnlyState.beginTopLevelExecution();
        try {
            executeElements(groupPlan.getElements(), PerformanceCoreTimerScope.empty(), iterationContext);
        } finally {
            defaultOnceOnlyState.endTopLevelExecution();
        }
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
        if (element instanceof PerformanceConditionController conditionController) {
            executeCondition(conditionController, scopedTimers, iterationContext);
            return;
        }
        if (element instanceof PerformanceWhileController whileController) {
            executeWhile(whileController, scopedTimers, iterationContext);
            return;
        }
        if (element instanceof PerformanceOnceOnlyController onceOnlyController) {
            executeOnceOnly(onceOnlyController, scopedTimers, iterationContext);
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

    private void executeCondition(PerformanceConditionController conditionController,
                                  PerformanceCoreTimerScope scopedTimers,
                                  C iterationContext) {
        if (!evaluateCondition(conditionController, iterationContext)) {
            return;
        }
        executeElements(conditionController.getElements(), scopedTimers, iterationContext);
    }

    private boolean evaluateCondition(PerformanceConditionController conditionController, C iterationContext) {
        try {
            return conditionEvaluator.evaluate(conditionController, iterationContext);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void executeWhile(PerformanceWhileController whileController,
                              PerformanceCoreTimerScope scopedTimers,
                              C iterationContext) {
        WhileData whileData = whileController.getWhileData();
        if (whileData == null) {
            whileData = new WhileData();
        }
        whileData.normalize();
        long deadline = whileData.timeoutMs <= 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + whileData.timeoutMs;
        for (int attempt = 0; attempt < whileData.maxIterations && runningSupplier.getAsBoolean(); attempt++) {
            if (deadline != Long.MAX_VALUE && System.currentTimeMillis() >= deadline) {
                return;
            }
            if (!evaluateWhile(whileController, iterationContext)) {
                return;
            }
            executeElements(whileController.getElements(), scopedTimers, iterationContext);
            if (!runningSupplier.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                return;
            }
            if (attempt + 1 >= whileData.maxIterations) {
                return;
            }
            if (deadline != Long.MAX_VALUE && System.currentTimeMillis() >= deadline) {
                return;
            }
            if (!evaluateWhile(whileController, iterationContext)) {
                return;
            }
            long delayMs = whileDelay(whileData.intervalMs, deadline);
            if (delayMs < 0) {
                return;
            }
            sleepDelay(delayMs);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }
    }

    private boolean evaluateWhile(PerformanceWhileController whileController, C iterationContext) {
        try {
            return whileEvaluator.evaluate(whileController, iterationContext);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private long whileDelay(long intervalMs, long deadline) {
        long delayMs = Math.max(0L, intervalMs);
        if (deadline == Long.MAX_VALUE) {
            return delayMs;
        }
        long remainingMs = deadline - System.currentTimeMillis();
        if (remainingMs <= 0L) {
            return -1L;
        }
        return Math.min(delayMs, remainingMs);
    }

    private void executeOnceOnly(PerformanceOnceOnlyController onceOnlyController,
                                 PerformanceCoreTimerScope scopedTimers,
                                 C iterationContext) {
        if (!enterOnceOnly(onceOnlyController, iterationContext)) {
            return;
        }
        executeElements(onceOnlyController.getElements(), scopedTimers, iterationContext);
    }

    private boolean enterOnceOnly(PerformanceOnceOnlyController onceOnlyController, C iterationContext) {
        try {
            return onceOnlyState.enter(onceOnlyController, iterationContext);
        } catch (RuntimeException ignored) {
            return false;
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
        sleepDelay(timerData.delayMs);
    }

    private void sleepDelay(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            timerSleeper.sleep(delayMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static <C> ConditionEvaluator<C> defaultConditionEvaluator() {
        return (conditionController, iterationContext) -> ConditionExpressionEvaluator.evaluate(
                conditionController.getConditionData() == null
                        ? null
                        : conditionController.getConditionData().expression,
                ConditionExpressionEvaluator.VariableLookup.EMPTY
        );
    }

    private static <C> WhileEvaluator<C> defaultWhileEvaluator() {
        return (whileController, iterationContext) -> ConditionExpressionEvaluator.evaluate(
                whileController.getWhileData() == null
                        ? null
                        : whileController.getWhileData().expression,
                ConditionExpressionEvaluator.VariableLookup.EMPTY
        );
    }

    @SuppressWarnings("unchecked")
    private static <C> DefaultOnceOnlyState<C> asDefaultOnceOnlyState(OnceOnlyState<C> onceOnlyState) {
        return onceOnlyState instanceof DefaultOnceOnlyState<?>
                ? (DefaultOnceOnlyState<C>) onceOnlyState
                : null;
    }

    private static final class DefaultOnceOnlyState<C> implements OnceOnlyState<C> {
        private final ThreadLocal<Set<String>> seenByExecution = new ThreadLocal<>();

        private void beginTopLevelExecution() {
            seenByExecution.set(ConcurrentHashMap.newKeySet());
        }

        private void endTopLevelExecution() {
            seenByExecution.remove();
        }

        @Override
        public boolean enter(PerformanceOnceOnlyController onceOnlyController, C iterationContext) {
            Set<String> seen = seenByExecution.get();
            if (seen == null) {
                seen = ConcurrentHashMap.newKeySet();
                seenByExecution.set(seen);
            }
            return seen.add(
                    System.identityHashCode(iterationContext) + ":" + System.identityHashCode(onceOnlyController)
            );
        }
    }
}

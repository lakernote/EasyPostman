package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.controller.ConditionExpressionEvaluator;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.plan.PerformanceConditionController;
import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceWhileController;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.variable.VariableResolver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

final class WebSocketScenarioPlanStepCursor {
    private final BooleanSupplier runningSupplier;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private PerformancePlanElement bufferedNext;

    WebSocketScenarioPlanStepCursor(PerformanceRequestSampler requestSampler, BooleanSupplier runningSupplier) {
        this.runningSupplier = runningSupplier;
        frames.push(new Frame(requestSampler == null ? List.of() : requestSampler.getChildren(), 1));
    }

    PerformancePlanElement next() {
        if (bufferedNext != null) {
            PerformancePlanElement next = bufferedNext;
            bufferedNext = null;
            return next;
        }
        return readNext();
    }

    PerformancePlanElement peek() {
        if (bufferedNext == null) {
            bufferedNext = readNext();
        }
        return bufferedNext;
    }

    private PerformancePlanElement readNext() {
        while (runningSupplier.getAsBoolean() && !frames.isEmpty()) {
            Frame frame = frames.peek();
            if (frame.index >= frame.elements.size()) {
                if (!frame.completeCurrentIteration(runningSupplier)) {
                    frames.pop();
                }
                continue;
            }

            PerformancePlanElement element = frame.elements.get(frame.index++);
            if (element instanceof PerformanceController controller) {
                if (!controller.getElements().isEmpty()) {
                    frames.push(new Frame(controller.getElements(), controller.getIterationCount()));
                }
                continue;
            }
            if (element instanceof PerformanceConditionController conditionController) {
                if (evaluateCondition(conditionController) && !conditionController.getElements().isEmpty()) {
                    frames.push(new Frame(conditionController.getElements(), 1));
                }
                continue;
            }
            if (element instanceof PerformanceWhileController whileController) {
                Frame whileFrame = Frame.whileFrame(whileController);
                if (whileFrame.shouldEnter()) {
                    frames.push(whileFrame);
                }
                continue;
            }
            return element;
        }
        return null;
    }

    void stop() {
        bufferedNext = null;
        frames.clear();
    }

    private boolean evaluateCondition(PerformanceConditionController conditionController) {
        try {
            return ConditionExpressionEvaluator.evaluate(
                    conditionController.getConditionData() == null
                            ? null
                            : conditionController.getConditionData().expression,
                    VARIABLE_LOOKUP
            );
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static final ConditionExpressionEvaluator.VariableLookup VARIABLE_LOOKUP =
            new ConditionExpressionEvaluator.VariableLookup() {
                @Override
                public String resolve(String variableName) {
                    return VariableResolver.resolveVariable(variableName);
                }

                @Override
                public boolean isDefined(String variableName) {
                    return VariableResolver.isVariableDefined(variableName);
                }
            };

    private static final class Frame {
        private final List<PerformancePlanElement> elements;
        private final int iterations;
        private final PerformanceWhileController whileController;
        private final WhileData whileData;
        private final long deadlineMs;
        private int index;
        private int completedIterations;

        private Frame(List<PerformancePlanElement> elements, int iterations) {
            this(elements, iterations, null, null, Long.MAX_VALUE);
        }

        private Frame(List<PerformancePlanElement> elements,
                      int iterations,
                      PerformanceWhileController whileController,
                      WhileData whileData,
                      long deadlineMs) {
            this.elements = elements == null ? List.of() : elements;
            this.iterations = Math.max(1, iterations);
            this.whileController = whileController;
            this.whileData = whileData;
            this.deadlineMs = deadlineMs;
        }

        private static Frame whileFrame(PerformanceWhileController whileController) {
            WhileData data = whileController == null ? null : whileController.getWhileData();
            if (data == null) {
                data = new WhileData();
            }
            data.normalize();
            long deadlineMs = data.timeoutMs <= 0
                    ? Long.MAX_VALUE
                    : System.currentTimeMillis() + data.timeoutMs;
            return new Frame(
                    whileController == null ? List.of() : whileController.getElements(),
                    data.maxIterations,
                    whileController,
                    data,
                    deadlineMs
            );
        }

        private boolean shouldEnter() {
            return whileController == null
                    || (!elements.isEmpty()
                    && !deadlineReached()
                    && evaluateWhile());
        }

        private boolean completeCurrentIteration(BooleanSupplier runningSupplier) {
            if (whileController == null) {
                completedIterations++;
                if (completedIterations < iterations) {
                    index = 0;
                    return true;
                }
                return false;
            }

            completedIterations++;
            if (completedIterations >= iterations
                    || !runningSupplier.getAsBoolean()
                    || deadlineReached()
                    || !evaluateWhile()) {
                return false;
            }
            if (!sleepInterval()) {
                return false;
            }
            if (deadlineReached() || !evaluateWhile()) {
                return false;
            }
            index = 0;
            return true;
        }

        private boolean deadlineReached() {
            return deadlineMs != Long.MAX_VALUE && System.currentTimeMillis() >= deadlineMs;
        }

        private boolean evaluateWhile() {
            try {
                return ConditionExpressionEvaluator.evaluate(
                        whileData == null ? null : whileData.expression,
                        VARIABLE_LOOKUP
                );
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        private boolean sleepInterval() {
            if (whileData == null || whileData.intervalMs <= 0) {
                return true;
            }
            long delayMs = whileData.intervalMs;
            if (deadlineMs != Long.MAX_VALUE) {
                long remainingMs = deadlineMs - System.currentTimeMillis();
                if (remainingMs <= 0L) {
                    return false;
                }
                delayMs = Math.min(delayMs, remainingMs);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);
                return true;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}

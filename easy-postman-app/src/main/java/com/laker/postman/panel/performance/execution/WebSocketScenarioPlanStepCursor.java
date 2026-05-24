package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.plan.PerformanceController;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;

final class WebSocketScenarioPlanStepCursor {
    private final BooleanSupplier runningSupplier;
    private final Deque<Frame> frames = new ArrayDeque<>();

    WebSocketScenarioPlanStepCursor(PerformanceRequestSampler requestSampler, BooleanSupplier runningSupplier) {
        this.runningSupplier = runningSupplier;
        frames.push(new Frame(requestSampler == null ? List.of() : requestSampler.getChildren(), 1));
    }

    PerformancePlanElement next() {
        while (runningSupplier.getAsBoolean() && !frames.isEmpty()) {
            Frame frame = frames.peek();
            if (frame.index >= frame.elements.size()) {
                frame.completedIterations++;
                if (frame.completedIterations < frame.iterations) {
                    frame.index = 0;
                } else {
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
            return element;
        }
        return null;
    }

    void stop() {
        frames.clear();
    }

    private static final class Frame {
        private final List<PerformancePlanElement> elements;
        private final int iterations;
        private int index;
        private int completedIterations;

        private Frame(List<PerformancePlanElement> elements, int iterations) {
            this.elements = elements == null ? List.of() : elements;
            this.iterations = Math.max(1, iterations);
        }
    }
}

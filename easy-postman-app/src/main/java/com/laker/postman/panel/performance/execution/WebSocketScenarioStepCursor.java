package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

final class WebSocketScenarioStepCursor {
    private final BooleanSupplier runningSupplier;
    private final Deque<Frame> frames = new ArrayDeque<>();

    WebSocketScenarioStepCursor(DefaultMutableTreeNode root, BooleanSupplier runningSupplier) {
        this.runningSupplier = runningSupplier;
        frames.push(new Frame(root, 1));
    }

    DefaultMutableTreeNode next() {
        while (runningSupplier.getAsBoolean() && !frames.isEmpty()) {
            Frame frame = frames.peek();
            if (frame.index >= frame.node.getChildCount()) {
                frame.completedIterations++;
                if (frame.completedIterations < frame.iterations) {
                    frame.index = 0;
                } else {
                    frames.pop();
                }
                continue;
            }

            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) frame.node.getChildAt(frame.index++);
            Object childObj = childNode.getUserObject();
            if (!(childObj instanceof JMeterTreeNode stepNode) || !stepNode.enabled) {
                continue;
            }
            if (stepNode.type == NodeType.LOOP) {
                LoopData loopData = stepNode.loopData != null ? stepNode.loopData : new LoopData();
                loopData.normalize();
                stepNode.loopData = loopData;
                if (childNode.getChildCount() > 0) {
                    frames.push(new Frame(childNode, loopData.iterations));
                }
                continue;
            }
            return childNode;
        }
        return null;
    }

    void stop() {
        frames.clear();
    }

    private static final class Frame {
        private final DefaultMutableTreeNode node;
        private final int iterations;
        private int index;
        private int completedIterations;

        private Frame(DefaultMutableTreeNode node, int iterations) {
            this.node = node;
            this.iterations = Math.max(1, iterations);
        }
    }
}

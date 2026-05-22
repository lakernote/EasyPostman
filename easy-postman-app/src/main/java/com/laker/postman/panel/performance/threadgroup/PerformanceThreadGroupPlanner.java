package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.NodeType;

import javax.swing.tree.DefaultMutableTreeNode;

public final class PerformanceThreadGroupPlanner {

    private static final double ESTIMATED_REQUEST_DURATION_SECONDS = 0.3;

    public int getTotalThreads(DefaultMutableTreeNode rootNode) {
        int total = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                if (!jtNode.enabled) {
                    continue;
                }
                ThreadGroupData tg = resolveThreadGroupData(jtNode);
                total += maxThreadCount(tg);
            }
        }
        return total;
    }

    public long estimateTotalRequests(DefaultMutableTreeNode rootNode) {
        long total = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObj = groupNode.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                if (!jtNode.enabled) {
                    continue;
                }

                ThreadGroupData tg = resolveThreadGroupData(jtNode);
                long enabledRequests = countEnabledRequestExecutions(groupNode);
                if (enabledRequests == 0) {
                    continue;
                }

                total = saturatingAdd(total, estimateThreadGroupRequests(tg, enabledRequests));
            }
        }
        return total;
    }

    private static int maxThreadCount(ThreadGroupData tg) {
        return switch (tg.threadMode) {
            case FIXED -> tg.numThreads;
            case RAMP_UP -> tg.rampUpEndThreads;
            case SPIKE -> tg.spikeMaxThreads;
            case STAIRS -> tg.stairsEndThreads;
        };
    }

    private static long estimateThreadGroupRequests(ThreadGroupData tg, long enabledRequests) {
        return switch (tg.threadMode) {
            case FIXED -> {
                if (tg.useTime) {
                    yield estimateTimedRequests(tg.numThreads, tg.duration, enabledRequests);
                }
                yield saturatingMultiply(saturatingMultiply(tg.numThreads, tg.loops), enabledRequests);
            }
            case RAMP_UP -> {
                int avgThreads = (tg.rampUpStartThreads + tg.rampUpEndThreads) / 2;
                yield estimateTimedRequests(avgThreads, tg.rampUpDuration, enabledRequests);
            }
            case SPIKE -> {
                int avgThreads = (tg.spikeMinThreads + tg.spikeMaxThreads) / 2;
                yield estimateTimedRequests(avgThreads, tg.spikeDuration, enabledRequests);
            }
            case STAIRS -> {
                int avgThreads = (tg.stairsStartThreads + tg.stairsEndThreads) / 2;
                yield estimateTimedRequests(avgThreads, tg.stairsDuration, enabledRequests);
            }
        };
    }

    private static long estimateTimedRequests(int threadCount, int durationSeconds, long enabledRequests) {
        double requestsPerSecondPerThread = 1.0 / ESTIMATED_REQUEST_DURATION_SECONDS;
        return saturatedDouble((double) threadCount * durationSeconds * requestsPerSecondPerThread * enabledRequests);
    }

    private static long countEnabledRequestExecutions(DefaultMutableTreeNode node) {
        long total = 0;
        for (int j = 0; j < node.getChildCount(); j++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(j);
            Object childObj = child.getUserObject();
            if (!(childObj instanceof JMeterTreeNode childJtNode) || !childJtNode.enabled) {
                continue;
            }
            if (childJtNode.type == NodeType.REQUEST) {
                total++;
                continue;
            }
            if (childJtNode.type == NodeType.LOOP) {
                LoopData loopData = childJtNode.loopData != null ? childJtNode.loopData : new LoopData();
                loopData.normalize();
                childJtNode.loopData = loopData;
                total = saturatingAdd(total, saturatingMultiply(loopData.iterations, countEnabledRequestExecutions(child)));
            }
        }
        return total;
    }

    private static long saturatingAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left == 0 || right == 0) {
            return 0;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private static long saturatedDouble(double value) {
        if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, (long) value);
    }

    public static ThreadGroupData resolveThreadGroupData(JMeterTreeNode node) {
        if (node.threadGroupData == null) {
            node.threadGroupData = new ThreadGroupData();
        }
        node.threadGroupData.normalize();
        return node.threadGroupData;
    }
}

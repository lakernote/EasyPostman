package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;

import javax.swing.tree.DefaultMutableTreeNode;

final class PerformanceThreadGroupPlanner {

    private static final double ESTIMATED_REQUEST_DURATION_SECONDS = 0.3;

    int getTotalThreads(DefaultMutableTreeNode rootNode) {
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

    long estimateTotalRequests(DefaultMutableTreeNode rootNode) {
        long total = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObj = groupNode.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                if (!jtNode.enabled) {
                    continue;
                }

                ThreadGroupData tg = resolveThreadGroupData(jtNode);
                int enabledRequests = countEnabledRequests(groupNode);
                if (enabledRequests == 0) {
                    continue;
                }

                total += estimateThreadGroupRequests(tg, enabledRequests);
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

    private static long estimateThreadGroupRequests(ThreadGroupData tg, int enabledRequests) {
        double requestsPerSecondPerThread = 1.0 / ESTIMATED_REQUEST_DURATION_SECONDS;
        return switch (tg.threadMode) {
            case FIXED -> {
                if (tg.useTime) {
                    yield (long) (tg.numThreads * tg.duration * requestsPerSecondPerThread * enabledRequests);
                }
                yield (long) tg.numThreads * tg.loops * enabledRequests;
            }
            case RAMP_UP -> {
                int avgThreads = (tg.rampUpStartThreads + tg.rampUpEndThreads) / 2;
                yield (long) (avgThreads * tg.rampUpDuration * requestsPerSecondPerThread * enabledRequests);
            }
            case SPIKE -> {
                int avgThreads = (tg.spikeMinThreads + tg.spikeMaxThreads) / 2;
                yield (long) (avgThreads * tg.spikeDuration * requestsPerSecondPerThread * enabledRequests);
            }
            case STAIRS -> {
                int avgThreads = (tg.stairsStartThreads + tg.stairsEndThreads) / 2;
                yield (long) (avgThreads * tg.stairsDuration * requestsPerSecondPerThread * enabledRequests);
            }
        };
    }

    private static int countEnabledRequests(DefaultMutableTreeNode groupNode) {
        int enabledRequests = 0;
        for (int j = 0; j < groupNode.getChildCount(); j++) {
            DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) groupNode.getChildAt(j);
            Object reqObj = requestNode.getUserObject();
            if (reqObj instanceof JMeterTreeNode reqJtNode
                    && reqJtNode.type == NodeType.REQUEST
                    && reqJtNode.enabled) {
                enabledRequests++;
            }
        }
        return enabledRequests;
    }

    static ThreadGroupData resolveThreadGroupData(JMeterTreeNode node) {
        if (node.threadGroupData == null) {
            node.threadGroupData = new ThreadGroupData();
        }
        node.threadGroupData.normalize();
        return node.threadGroupData;
    }
}

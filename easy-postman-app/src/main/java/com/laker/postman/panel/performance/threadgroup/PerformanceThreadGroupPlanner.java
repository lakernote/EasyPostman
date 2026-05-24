package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.panel.performance.plan.PerformanceController;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceTestPlan;
import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;

import java.util.List;

public final class PerformanceThreadGroupPlanner {

    private static final double ESTIMATED_REQUEST_DURATION_SECONDS = 0.3;

    public int getTotalThreads(PerformanceTestPlan plan) {
        int total = 0;
        if (plan == null) {
            return total;
        }
        for (PerformanceThreadGroupPlan groupPlan : plan.getThreadGroups()) {
            total += maxThreadCount(groupPlan.getThreadGroupData());
        }
        return total;
    }

    public long estimateTotalRequests(PerformanceTestPlan plan) {
        long total = 0;
        if (plan == null) {
            return total;
        }
        for (PerformanceThreadGroupPlan groupPlan : plan.getThreadGroups()) {
            long enabledRequests = countEnabledRequestExecutions(groupPlan.getElements());
            if (enabledRequests == 0) {
                continue;
            }

            total = saturatingAdd(total, estimateThreadGroupRequests(groupPlan.getThreadGroupData(), enabledRequests));
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

    private static long countEnabledRequestExecutions(List<PerformancePlanElement> elements) {
        long total = 0;
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceRequestSampler) {
                total++;
                continue;
            }
            if (element instanceof PerformanceController controller) {
                total = saturatingAdd(
                        total,
                        saturatingMultiply(
                                controller.getIterationCount(),
                                countEnabledRequestExecutions(controller.getElements())
                        )
                );
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

}

package com.laker.postman.performance.core.threadgroup;

import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;

import java.util.List;

public final class PerformanceCoreThreadGroupPlanner {

    private static final double ESTIMATED_REQUEST_DURATION_SECONDS = 0.3;

    public int getTotalThreads(PerformanceTestPlan plan) {
        int total = 0;
        if (plan == null) {
            return total;
        }
        for (PerformanceThreadGroupPlan groupPlan : plan.getThreadGroups()) {
            total = saturatingAddInt(total, maxThreadCount(resolveThreadGroupData(groupPlan)));
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

            total = saturatingAdd(total, estimateThreadGroupRequests(resolveThreadGroupData(groupPlan), enabledRequests));
        }
        return total;
    }

    private static ThreadGroupData resolveThreadGroupData(PerformanceThreadGroupPlan groupPlan) {
        ThreadGroupData threadGroupData = groupPlan == null ? null : groupPlan.getThreadGroupData();
        if (threadGroupData == null) {
            threadGroupData = new ThreadGroupData();
        }
        threadGroupData.normalize();
        return threadGroupData;
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
                int avgThreads = averageThreadCount(tg.rampUpStartThreads, tg.rampUpEndThreads);
                yield estimateTimedRequests(avgThreads, tg.rampUpDuration, enabledRequests);
            }
            case SPIKE -> {
                int avgThreads = averageThreadCount(tg.spikeMinThreads, tg.spikeMaxThreads);
                yield estimateTimedRequests(avgThreads, tg.spikeDuration, enabledRequests);
            }
            case STAIRS -> {
                int avgThreads = averageThreadCount(tg.stairsStartThreads, tg.stairsEndThreads);
                yield estimateTimedRequests(avgThreads, tg.stairsDuration, enabledRequests);
            }
        };
    }

    private static int averageThreadCount(int left, int right) {
        return (int) (((long) left + right) / 2L);
    }

    private static long estimateTimedRequests(int threadCount, int durationSeconds, long enabledRequests) {
        double requestsPerSecondPerThread = 1.0 / ESTIMATED_REQUEST_DURATION_SECONDS;
        return saturatedDouble((double) threadCount * durationSeconds * requestsPerSecondPerThread * enabledRequests);
    }

    private static long countEnabledRequestExecutions(List<PerformancePlanElement> elements) {
        long total = 0;
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceSampler) {
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

    private static int saturatingAddInt(int left, int right) {
        if (Integer.MAX_VALUE - left < right) {
            return Integer.MAX_VALUE;
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

package com.laker.postman.performance.core.threadgroup;

import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.plan.PerformanceConditionController;
import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformanceElementContainer;
import com.laker.postman.performance.core.plan.PerformanceOnceOnlyController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceWhileController;
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
            if (groupPlan == null) {
                continue;
            }
            total = saturatingAddInt(total, maxThreadCount(resolveThreadGroupData(groupPlan)));
        }
        return total;
    }

    public long estimateTotalRequests(PerformanceTestPlan plan) {
        return estimateRequestCount(plan).estimatedRequests();
    }

    public PerformanceRequestEstimate estimateRequestCount(PerformanceTestPlan plan) {
        long total = 0;
        boolean dynamic = false;
        if (plan == null) {
            return PerformanceRequestEstimate.fixed(total);
        }
        for (PerformanceThreadGroupPlan groupPlan : plan.getThreadGroups()) {
            if (groupPlan == null) {
                continue;
            }
            ElementRequestCounts requestCounts = countEnabledRequestExecutions(groupPlan.getElements());
            ThreadGroupData threadGroupData = resolveThreadGroupData(groupPlan);
            long regularRequests = estimateThreadGroupRequests(threadGroupData, requestCounts.perIterationRequests());
            long onceOnlyRequests = saturatingMultiply(maxThreadCount(threadGroupData), requestCounts.oncePerVirtualUserRequests());
            total = saturatingAdd(total, saturatingAdd(regularRequests, onceOnlyRequests));
            dynamic |= requestCounts.dynamic();
        }
        return dynamic ? PerformanceRequestEstimate.dynamic(total) : PerformanceRequestEstimate.fixed(total);
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

    private static ElementRequestCounts countEnabledRequestExecutions(List<PerformancePlanElement> elements) {
        ElementRequestCounts total = ElementRequestCounts.zero();
        if (elements == null) {
            return total;
        }
        for (PerformancePlanElement element : elements) {
            total = total.add(countEnabledRequestExecutions(element));
        }
        return total;
    }

    private static ElementRequestCounts countEnabledRequestExecutions(PerformancePlanElement element) {
        if (element == null) {
            return ElementRequestCounts.zero();
        }
        if (element instanceof PerformanceSampler) {
            return ElementRequestCounts.perIteration(1L);
        }
        if (element instanceof PerformanceConditionController conditionController) {
            return countEnabledRequestExecutions(conditionController.getElements()).asDynamic();
        }
        if (element instanceof PerformanceWhileController whileController) {
            WhileData whileData = whileController.getWhileData();
            if (whileData == null) {
                whileData = new WhileData();
            }
            whileData.normalize();
            return countEnabledRequestExecutions(whileController.getElements())
                    .repeat(whileData.maxIterations)
                    .asDynamic();
        }
        if (element instanceof PerformanceOnceOnlyController onceOnlyController) {
            return countEnabledRequestExecutions(onceOnlyController.getElements()).oncePerVirtualUser();
        }
        if (element instanceof PerformanceController controller) {
            return countEnabledRequestExecutions(controller.getElements()).repeat(controller.getIterationCount());
        }
        if (element instanceof PerformanceElementContainer container) {
            return countEnabledRequestExecutions(container.getElements());
        }
        return ElementRequestCounts.zero();
    }

    private record ElementRequestCounts(long perIterationRequests,
                                        long oncePerVirtualUserRequests,
                                        boolean dynamic) {
        private static ElementRequestCounts zero() {
            return new ElementRequestCounts(0L, 0L, false);
        }

        private static ElementRequestCounts perIteration(long requests) {
            return new ElementRequestCounts(Math.max(0L, requests), 0L, false);
        }

        private ElementRequestCounts add(ElementRequestCounts other) {
            if (other == null) {
                return this;
            }
            return new ElementRequestCounts(
                    saturatingAdd(perIterationRequests, other.perIterationRequests),
                    saturatingAdd(oncePerVirtualUserRequests, other.oncePerVirtualUserRequests),
                    dynamic || other.dynamic
            );
        }

        private ElementRequestCounts repeat(int iterations) {
            return new ElementRequestCounts(
                    saturatingMultiply(Math.max(0, iterations), perIterationRequests),
                    oncePerVirtualUserRequests,
                    dynamic
            );
        }

        private ElementRequestCounts asDynamic() {
            return new ElementRequestCounts(perIterationRequests, oncePerVirtualUserRequests, true);
        }

        private ElementRequestCounts oncePerVirtualUser() {
            return new ElementRequestCounts(
                    0L,
                    saturatingAdd(perIterationRequests, oncePerVirtualUserRequests),
                    dynamic
            );
        }
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

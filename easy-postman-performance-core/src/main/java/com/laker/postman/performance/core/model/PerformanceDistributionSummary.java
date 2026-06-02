package com.laker.postman.performance.core.model;

import java.util.concurrent.atomic.LongAdder;

/**
 * 数值分布摘要，对应 Micrometer DistributionSummary 语义，适合记录字节数、消息数等非耗时数量。
 */
final class PerformanceDistributionSummary {
    private final LongAdder count = new LongAdder();
    private final LongAdder totalAmount = new LongAdder();

    void record(long amount) {
        long normalized = Math.max(0L, amount);
        totalAmount.add(normalized);
        count.increment();
    }

    long count() {
        return count.sum();
    }

    long totalAmount() {
        return totalAmount.sum();
    }

    double mean() {
        long currentCount = count();
        return currentCount == 0 ? Double.NaN : PerformanceMetricMath.round((double) totalAmount() / currentCount);
    }

    long avg() {
        long currentCount = count();
        return currentCount == 0 ? 0 : totalAmount() / currentCount;
    }

    void clear() {
        count.reset();
        totalAmount.reset();
    }
}

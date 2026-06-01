package com.laker.postman.performance.core.model;

/**
 * 数值分布摘要，对应 Micrometer DistributionSummary 语义，适合记录字节数、消息数等非耗时数量。
 */
final class PerformanceDistributionSummary {
    private long count;
    private long totalAmount;

    void record(long amount) {
        long normalized = Math.max(0L, amount);
        count++;
        totalAmount += normalized;
    }

    long count() {
        return count;
    }

    long totalAmount() {
        return totalAmount;
    }

    double mean() {
        return count == 0 ? Double.NaN : PerformanceMetricMath.round((double) totalAmount / count);
    }

    long avg() {
        return count == 0 ? 0 : totalAmount / count;
    }

    void clear() {
        count = 0;
        totalAmount = 0;
    }
}

package com.laker.postman.performance.core.model;

public record PerformanceStatsProgressSnapshot(
        long totalRequests,
        long successRequests,
        long failedRequests,
        double qps
) {
    public static PerformanceStatsProgressSnapshot empty() {
        return new PerformanceStatsProgressSnapshot(0, 0, 0, 0.0);
    }
}

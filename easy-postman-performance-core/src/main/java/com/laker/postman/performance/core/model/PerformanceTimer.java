package com.laker.postman.performance.core.model;

/**
 * 耗时计量器，对应 Micrometer Timer 语义：记录次数、总耗时和分布快照。
 */
final class PerformanceTimer {
    private final DurationStatsHistogram histogram = new DurationStatsHistogram();
    private long count;
    private long totalTimeMs;

    void record(long durationMs) {
        long normalized = Math.max(0L, durationMs);
        count++;
        totalTimeMs += normalized;
        histogram.record(normalized);
    }

    long count() {
        return count;
    }

    long totalTimeMs() {
        return totalTimeMs;
    }

    double meanMs() {
        return count == 0 ? Double.NaN : PerformanceMetricMath.round((double) totalTimeMs / count);
    }

    long avgMs() {
        return count == 0 ? 0 : totalTimeMs / count;
    }

    PerformanceStatsSnapshot.DurationStats snapshot() {
        return histogram.snapshot();
    }

    void clear() {
        count = 0;
        totalTimeMs = 0;
        histogram.clear();
    }
}

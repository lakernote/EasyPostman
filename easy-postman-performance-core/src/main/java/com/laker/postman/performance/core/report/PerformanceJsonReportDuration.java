package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceJsonReportDuration {
    long avg;
    long min;
    long max;
    long p90;
    long p95;
    long p99;

    @Builder
    public PerformanceJsonReportDuration(Long avg, Long min, Long max, Long p90, Long p95, Long p99) {
        this.avg = Math.max(0L, avg == null ? 0L : avg);
        this.min = Math.max(0L, min == null ? 0L : min);
        this.max = Math.max(0L, max == null ? 0L : max);
        this.p90 = Math.max(0L, p90 == null ? 0L : p90);
        this.p95 = Math.max(0L, p95 == null ? 0L : p95);
        this.p99 = Math.max(0L, p99 == null ? 0L : p99);
    }
}

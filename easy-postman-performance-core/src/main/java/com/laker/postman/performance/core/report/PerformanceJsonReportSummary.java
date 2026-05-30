package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceJsonReportSummary {
    long totalRequests;
    long successRequests;
    long failedRequests;
    double successRate;

    @Builder
    public PerformanceJsonReportSummary(Long totalRequests,
                                        Long successRequests,
                                        Long failedRequests,
                                        Double successRate) {
        this.totalRequests = Math.max(0L, totalRequests == null ? 0L : totalRequests);
        this.successRequests = Math.max(0L, successRequests == null ? 0L : successRequests);
        this.failedRequests = Math.max(0L, failedRequests == null ? this.totalRequests - this.successRequests : failedRequests);
        this.successRate = successRate == null || !Double.isFinite(successRate)
                ? this.totalRequests == 0 ? 0D : this.successRequests * 100D / this.totalRequests
                : successRate;
    }
}

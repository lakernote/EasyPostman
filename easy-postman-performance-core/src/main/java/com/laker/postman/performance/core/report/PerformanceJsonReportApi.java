package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceJsonReportApi {
    String apiId;
    String name;
    String protocol;
    long total;
    long success;
    long failed;
    double successRate;
    double samplesPerSecond;
    PerformanceJsonReportDuration durationMs;
    PerformanceJsonReportStream stream;
    PerformanceJsonReportDuration firstMessageLatencyMs;

    @Builder
    public PerformanceJsonReportApi(String apiId,
                                    String name,
                                    String protocol,
                                    Long total,
                                    Long success,
                                    Long failed,
                                    Double successRate,
                                    Double samplesPerSecond,
                                    PerformanceJsonReportDuration durationMs,
                                    PerformanceJsonReportStream stream,
                                    PerformanceJsonReportDuration firstMessageLatencyMs) {
        this.apiId = apiId == null ? "" : apiId;
        this.name = name == null ? "" : name;
        this.protocol = protocol == null ? "" : protocol;
        this.total = Math.max(0L, total == null ? 0L : total);
        this.success = Math.max(0L, success == null ? 0L : success);
        this.failed = Math.max(0L, failed == null ? this.total - this.success : failed);
        this.successRate = successRate == null || !Double.isFinite(successRate)
                ? this.total == 0 ? 0D : this.success * 100D / this.total
                : successRate;
        this.samplesPerSecond = samplesPerSecond == null || !Double.isFinite(samplesPerSecond) ? 0D : samplesPerSecond;
        this.durationMs = durationMs == null ? PerformanceJsonReportDuration.builder().build() : durationMs;
        this.stream = stream == null ? PerformanceJsonReportStream.builder().build() : stream;
        this.firstMessageLatencyMs = firstMessageLatencyMs == null
                ? PerformanceJsonReportDuration.builder().build()
                : firstMessageLatencyMs;
    }
}

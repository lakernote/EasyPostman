package com.laker.postman.performance.core.model;

record PerformanceSampleMeterSnapshot(
        String apiId,
        String apiName,
        PerformanceProtocol protocol,
        long total,
        long success,
        long fail,
        long sentMessages,
        long receivedMessages,
        long matchedMessages,
        long sentBytes,
        long receivedBytes,
        long firstSampleStartTimeMs,
        long lastSampleEndTimeMs,
        double sampleSpanSeconds,
        long avgReceivedBytes,
        long avgDurationMs,
        PerformanceStatsSnapshot.DurationStats durationStats,
        double avgFirstMessageLatencyMs,
        long avgFirstMessageLatencyRoundedMs,
        PerformanceStatsSnapshot.DurationStats firstMessageLatencyStats
) {
    static PerformanceSampleMeterSnapshot empty(String apiId, String apiName, PerformanceProtocol protocol) {
        return new PerformanceSampleMeterSnapshot(
                apiId,
                apiName,
                protocol == null ? PerformanceProtocol.HTTP : protocol,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                PerformanceStatsSnapshot.DurationStats.empty(),
                Double.NaN,
                0,
                PerformanceStatsSnapshot.DurationStats.empty()
        );
    }
}

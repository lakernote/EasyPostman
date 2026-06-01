package com.laker.postman.performance.core.model;

final class PerformanceSampleMeterSet {
    private final String apiId;
    private final PerformanceProtocol protocol;
    private final PerformanceCounter total = new PerformanceCounter();
    private final PerformanceCounter success = new PerformanceCounter();
    private final PerformanceCounter sentMessages = new PerformanceCounter();
    private final PerformanceCounter receivedMessages = new PerformanceCounter();
    private final PerformanceCounter matchedMessages = new PerformanceCounter();
    private final PerformanceDistributionSummary sentBytes = new PerformanceDistributionSummary();
    private final PerformanceDistributionSummary receivedBytes = new PerformanceDistributionSummary();
    private final PerformanceTimer durations = new PerformanceTimer();
    private final PerformanceTimer firstMessageLatencies = new PerformanceTimer();
    private final PerformanceSampleTimeWindow sampleWindow = new PerformanceSampleTimeWindow();
    private String apiName;

    PerformanceSampleMeterSet(String apiId, PerformanceProtocol protocol) {
        this.apiId = apiId == null ? "" : apiId;
        this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
    }

    synchronized void record(RequestResult result) {
        total.increment();
        if (result.success) {
            success.increment();
        }
        sampleWindow.record(result.startTime, result.endTime);
        durations.record(result.getResponseTime());
        sentMessages.increment(result.sentMessages);
        receivedMessages.increment(result.receivedMessages);
        matchedMessages.increment(result.matchedMessages);
        sentBytes.record(result.sentBytes);
        receivedBytes.record(result.receivedBytes);
        if ((apiName == null || apiName.isBlank()) && result.apiName != null && !result.apiName.isBlank()) {
            apiName = result.apiName;
        }
        if (result.firstMessageLatencyMs >= 0) {
            firstMessageLatencies.record(result.firstMessageLatencyMs);
        }
    }

    synchronized void clear() {
        total.clear();
        success.clear();
        sentMessages.clear();
        receivedMessages.clear();
        matchedMessages.clear();
        sentBytes.clear();
        receivedBytes.clear();
        sampleWindow.clear();
        apiName = null;
        firstMessageLatencies.clear();
        durations.clear();
    }

    synchronized String apiName() {
        String displayName = resolvedApiName();
        return displayName.isBlank() ? apiId : displayName;
    }

    synchronized PerformanceSampleMeterSnapshot snapshot() {
        String displayName = resolvedApiName();
        long sampleCount = total.count();
        if (sampleCount == 0) {
            return PerformanceSampleMeterSnapshot.empty(apiId, displayName, protocol);
        }
        return new PerformanceSampleMeterSnapshot(
                apiId,
                displayName,
                protocol,
                sampleCount,
                success.count(),
                sampleCount - success.count(),
                sentMessages.count(),
                receivedMessages.count(),
                matchedMessages.count(),
                sentBytes.totalAmount(),
                receivedBytes.totalAmount(),
                sampleWindow.firstStartMs(),
                sampleWindow.lastEndMs(),
                sampleWindow.spanSeconds(),
                receivedBytes.avg(),
                durations.avgMs(),
                durations.snapshot(),
                firstMessageLatencies.count() == 0 ? Double.NaN : firstMessageLatencies.meanMs(),
                firstMessageLatencies.avgMs(),
                firstMessageLatencies.snapshot()
        );
    }

    synchronized PerformanceStatsSnapshot.ApiSummary toSummary(String name) {
        PerformanceSampleMeterSnapshot snapshot = snapshot();
        String summaryName = name;
        if ((summaryName == null || summaryName.isBlank()) && snapshot.apiName() != null && !snapshot.apiName().isBlank()) {
            summaryName = snapshot.apiName();
        }
        double spanSeconds = snapshot.sampleSpanSeconds();
        return new PerformanceStatsSnapshot.ApiSummary(
                snapshot.apiId(),
                summaryName,
                snapshot.protocol(),
                snapshot.total(),
                snapshot.success(),
                snapshot.fail(),
                snapshot.total() > 0 ? snapshot.success() * 100.0 / snapshot.total() : 0,
                PerformanceMetricMath.rate(snapshot.total(), spanSeconds),
                snapshot.firstSampleStartTimeMs(),
                snapshot.lastSampleEndTimeMs(),
                snapshot.durationStats(),
                snapshot.sentMessages(),
                snapshot.receivedMessages(),
                snapshot.matchedMessages(),
                PerformanceMetricMath.rate(snapshot.sentMessages(), spanSeconds),
                PerformanceMetricMath.rate(snapshot.receivedMessages(), spanSeconds),
                PerformanceMetricMath.rate(snapshot.matchedMessages(), spanSeconds),
                snapshot.sentBytes(),
                snapshot.receivedBytes(),
                PerformanceMetricMath.rate(snapshot.sentBytes(), spanSeconds),
                PerformanceMetricMath.rate(snapshot.receivedBytes(), spanSeconds),
                snapshot.avgReceivedBytes(),
                snapshot.avgFirstMessageLatencyRoundedMs(),
                snapshot.firstMessageLatencyStats()
        );
    }

    synchronized PerformanceStatsProgressSnapshot toProgressSnapshot() {
        PerformanceSampleMeterSnapshot snapshot = snapshot();
        if (snapshot.total() == 0) {
            return PerformanceStatsProgressSnapshot.empty();
        }
        return new PerformanceStatsProgressSnapshot(
                snapshot.total(),
                snapshot.success(),
                snapshot.fail(),
                PerformanceMetricMath.rate(snapshot.total(), snapshot.sampleSpanSeconds())
        );
    }

    private String resolvedApiName() {
        if (apiName != null && !apiName.isBlank()) {
            return apiName;
        }
        return apiId;
    }
}

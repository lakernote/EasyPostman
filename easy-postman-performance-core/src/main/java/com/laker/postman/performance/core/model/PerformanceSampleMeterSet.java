package com.laker.postman.performance.core.model;

import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<String> apiName = new AtomicReference<>("");

    PerformanceSampleMeterSet(String apiId, PerformanceProtocol protocol) {
        this.apiId = apiId == null ? "" : apiId;
        this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
    }

    void record(RequestResult result) {
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
        if (result.apiName != null && !result.apiName.isBlank()) {
            apiName.compareAndSet("", result.apiName);
        }
        if (result.firstMessageLatencyMs >= 0) {
            firstMessageLatencies.record(result.firstMessageLatencyMs);
        }
        total.increment();
    }

    void clear() {
        total.clear();
        success.clear();
        sentMessages.clear();
        receivedMessages.clear();
        matchedMessages.clear();
        sentBytes.clear();
        receivedBytes.clear();
        sampleWindow.clear();
        apiName.set("");
        firstMessageLatencies.clear();
        durations.clear();
    }

    String apiName() {
        String displayName = resolvedApiName();
        return displayName.isBlank() ? apiId : displayName;
    }

    PerformanceSampleMeterSnapshot snapshot() {
        String displayName = resolvedApiName();
        long sampleCount = total.count();
        if (sampleCount == 0) {
            return PerformanceSampleMeterSnapshot.empty(apiId, displayName, protocol);
        }
        long successCount = Math.min(success.count(), sampleCount);
        return new PerformanceSampleMeterSnapshot(
                apiId,
                displayName,
                protocol,
                sampleCount,
                successCount,
                sampleCount - successCount,
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

    PerformanceStatsSnapshot.ApiSummary toSummary(String name) {
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

    PerformanceStatsProgressSnapshot toProgressSnapshot() {
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
        String currentApiName = apiName.get();
        if (currentApiName != null && !currentApiName.isBlank()) {
            return currentApiName;
        }
        return apiId;
    }
}

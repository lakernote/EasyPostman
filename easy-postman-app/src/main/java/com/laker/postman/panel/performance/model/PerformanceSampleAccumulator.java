package com.laker.postman.panel.performance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class PerformanceSampleAccumulator {
    private final String apiId;
    private final PerformanceProtocol protocol;
    private final DurationStatsHistogram durations = new DurationStatsHistogram();
    private final DurationStatsHistogram firstMessageLatencies = new DurationStatsHistogram();
    private long total;
    private long success;
    private long firstStart = Long.MAX_VALUE;
    private long lastEnd;
    private long sentMessages;
    private long receivedMessages;
    private long matchedMessages;
    private long firstMessageLatencyTotal;
    private long firstMessageLatencyCount;

    PerformanceSampleAccumulator(String apiId, PerformanceProtocol protocol) {
        this.apiId = apiId;
        this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
    }

    void record(RequestResult result) {
        total++;
        if (result.success) {
            success++;
        }
        firstStart = Math.min(firstStart, result.startTime);
        lastEnd = Math.max(lastEnd, result.endTime);
        durations.record(result.getResponseTime());
        sentMessages += Math.max(0, result.sentMessages);
        receivedMessages += Math.max(0, result.receivedMessages);
        matchedMessages += Math.max(0, result.matchedMessages);
        if (result.firstMessageLatencyMs >= 0) {
            firstMessageLatencyTotal += result.firstMessageLatencyMs;
            firstMessageLatencyCount++;
            firstMessageLatencies.record(result.firstMessageLatencyMs);
        }
    }

    void clear() {
        total = 0;
        success = 0;
        firstStart = Long.MAX_VALUE;
        lastEnd = 0;
        sentMessages = 0;
        receivedMessages = 0;
        matchedMessages = 0;
        firstMessageLatencyTotal = 0;
        firstMessageLatencyCount = 0;
        firstMessageLatencies.clear();
        durations.clear();
    }

    String apiId() {
        return apiId;
    }

    long total() {
        return total;
    }

    long success() {
        return success;
    }

    long fail() {
        return total - success;
    }

    long sentMessages() {
        return sentMessages;
    }

    long receivedMessages() {
        return receivedMessages;
    }

    long matchedMessages() {
        return matchedMessages;
    }

    long avgDurationMs() {
        return durations.avg();
    }

    double avgFirstMessageLatencyMs() {
        return firstMessageLatencyCount == 0
                ? Double.NaN
                : round((double) firstMessageLatencyTotal / firstMessageLatencyCount);
    }

    PerformanceStatsSnapshot.ApiSummary toSummary(String name) {
        double spanSeconds = total == 0 ? 0 : Math.max(0.001, (lastEnd - firstStart) / 1000.0);
        double sendRate = spanSeconds > 0 ? round(sentMessages / spanSeconds) : 0;
        double receiveRate = spanSeconds > 0 ? round(receivedMessages / spanSeconds) : 0;
        double matchedRate = spanSeconds > 0 ? round(matchedMessages / spanSeconds) : 0;
        return new PerformanceStatsSnapshot.ApiSummary(
                apiId,
                name,
                protocol,
                total,
                success,
                fail(),
                total > 0 ? success * 100.0 / total : 0,
                spanSeconds > 0 ? round(total / spanSeconds) : 0,
                durations.snapshot(),
                sentMessages,
                receivedMessages,
                matchedMessages,
                sendRate,
                receiveRate,
                matchedRate,
                firstMessageLatencyCount == 0 ? 0 : firstMessageLatencyTotal / firstMessageLatencyCount,
                firstMessageLatencies.snapshot()
        );
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

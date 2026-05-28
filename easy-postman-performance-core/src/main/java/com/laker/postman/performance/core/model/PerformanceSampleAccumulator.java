package com.laker.postman.performance.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class PerformanceSampleAccumulator {
    private final String apiId;
    private final PerformanceProtocol protocol;
    private final DurationStatsHistogram durations = new DurationStatsHistogram();
    private final DurationStatsHistogram firstMessageLatencies = new DurationStatsHistogram();
    private String apiName;
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

    synchronized void record(RequestResult result) {
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
        if ((apiName == null || apiName.isBlank()) && result.apiName != null && !result.apiName.isBlank()) {
            apiName = result.apiName;
        }
        if (result.firstMessageLatencyMs >= 0) {
            firstMessageLatencyTotal += result.firstMessageLatencyMs;
            firstMessageLatencyCount++;
            firstMessageLatencies.record(result.firstMessageLatencyMs);
        }
    }

    synchronized void clear() {
        total = 0;
        success = 0;
        firstStart = Long.MAX_VALUE;
        lastEnd = 0;
        sentMessages = 0;
        receivedMessages = 0;
        matchedMessages = 0;
        firstMessageLatencyTotal = 0;
        firstMessageLatencyCount = 0;
        apiName = null;
        firstMessageLatencies.clear();
        durations.clear();
    }

    synchronized String apiId() {
        return apiId;
    }

    synchronized String apiName() {
        if (apiName != null && !apiName.isBlank()) {
            return apiName;
        }
        return apiId;
    }

    synchronized long total() {
        return total;
    }

    synchronized long success() {
        return success;
    }

    synchronized long fail() {
        return total - success;
    }

    synchronized long sentMessages() {
        return sentMessages;
    }

    synchronized long receivedMessages() {
        return receivedMessages;
    }

    synchronized long matchedMessages() {
        return matchedMessages;
    }

    synchronized long avgDurationMs() {
        return durations.avg();
    }

    synchronized double avgFirstMessageLatencyMs() {
        return firstMessageLatencyCount == 0
                ? Double.NaN
                : round((double) firstMessageLatencyTotal / firstMessageLatencyCount);
    }

    synchronized PerformanceStatsSnapshot.ApiSummary toSummary(String name) {
        String summaryName = name;
        if ((summaryName == null || summaryName.isBlank()) && apiName != null && !apiName.isBlank()) {
            summaryName = apiName;
        }
        double spanSeconds = total == 0 ? 0 : Math.max(0.001, (lastEnd - firstStart) / 1000.0);
        double sendRate = spanSeconds > 0 ? round(sentMessages / spanSeconds) : 0;
        double receiveRate = spanSeconds > 0 ? round(receivedMessages / spanSeconds) : 0;
        double matchedRate = spanSeconds > 0 ? round(matchedMessages / spanSeconds) : 0;
        return new PerformanceStatsSnapshot.ApiSummary(
                apiId,
                summaryName,
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

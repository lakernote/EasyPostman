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
    // 样本窗口开始/结束时间：QPS 和 byte/s 都用这个窗口计算，避免用运行总耗时稀释短请求吞吐。
    private long firstStart = Long.MAX_VALUE;
    private long lastEnd;
    private long sentMessages;
    private long receivedMessages;
    private long matchedMessages;
    // HTTP 字节总量：请求头+请求体、响应头+响应体，用于派生 Sent/Received KB/s。
    private long sentBytes;
    private long receivedBytes;
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
        sentBytes += Math.max(0, result.sentBytes);
        receivedBytes += Math.max(0, result.receivedBytes);
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
        sentBytes = 0;
        receivedBytes = 0;
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

    synchronized long sentBytes() {
        return sentBytes;
    }

    synchronized long receivedBytes() {
        return receivedBytes;
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
        double sentBytesPerSecond = spanSeconds > 0 ? round(sentBytes / spanSeconds) : 0;
        double receivedBytesPerSecond = spanSeconds > 0 ? round(receivedBytes / spanSeconds) : 0;
        return new PerformanceStatsSnapshot.ApiSummary(
                apiId,
                summaryName,
                protocol,
                total,
                success,
                fail(),
                total > 0 ? success * 100.0 / total : 0,
                spanSeconds > 0 ? round(total / spanSeconds) : 0,
                total == 0 ? 0 : firstStart,
                total == 0 ? 0 : lastEnd,
                durations.snapshot(),
                sentMessages,
                receivedMessages,
                matchedMessages,
                sendRate,
                receiveRate,
                matchedRate,
                sentBytes,
                receivedBytes,
                sentBytesPerSecond,
                receivedBytesPerSecond,
                total == 0 ? 0 : receivedBytes / total,
                firstMessageLatencyCount == 0 ? 0 : firstMessageLatencyTotal / firstMessageLatencyCount,
                firstMessageLatencies.snapshot()
        );
    }

    synchronized PerformanceStatsProgressSnapshot toProgressSnapshot() {
        if (total == 0) {
            return PerformanceStatsProgressSnapshot.empty();
        }
        double spanSeconds = Math.max(0.001, (lastEnd - firstStart) / 1000.0);
        return new PerformanceStatsProgressSnapshot(
                total,
                success,
                fail(),
                round(total / spanSeconds)
        );
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

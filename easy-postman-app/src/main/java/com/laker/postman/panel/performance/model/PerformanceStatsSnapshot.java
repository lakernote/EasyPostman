package com.laker.postman.panel.performance.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PerformanceStatsSnapshot {

    private final List<ApiSummary> summaries;
    private final Map<PerformanceProtocol, ApiSummary> totalsByProtocol;
    private final long totalRequests;
    private final long successRequests;
    private final long retainedRequestResultCount;

    PerformanceStatsSnapshot(List<ApiSummary> summaries,
                             Map<PerformanceProtocol, ApiSummary> totalsByProtocol,
                             long totalRequests,
                             long successRequests,
                             long retainedRequestResultCount) {
        this.summaries = List.copyOf(summaries);
        this.totalsByProtocol = new EnumMap<>(PerformanceProtocol.class);
        this.totalsByProtocol.putAll(totalsByProtocol);
        this.totalRequests = totalRequests;
        this.successRequests = successRequests;
        this.retainedRequestResultCount = retainedRequestResultCount;
    }

    public List<ApiSummary> summaries() {
        return summaries;
    }

    public long totalRequests() {
        return totalRequests;
    }

    public long successRequests() {
        return successRequests;
    }

    public long retainedRequestResultCount() {
        return retainedRequestResultCount;
    }

    public ApiSummary totalFor(PerformanceProtocol protocol, String name) {
        ApiSummary summary = totalsByProtocol.get(protocol);
        return summary == null ? null : summary.withName(name);
    }

    public record ApiSummary(
            String apiId,
            String name,
            PerformanceProtocol protocol,
            long total,
            long success,
            long fail,
            double successRate,
            double samplesPerSecond,
            DurationStats durationStats,
            long sentMessages,
            long receivedMessages,
            long matchedMessages,
            double sendRate,
            double receiveRate,
            double matchedRate,
            long avgFirstMessageLatencyMs,
            DurationStats firstMessageLatencyStats,
            String topCompletionReason
    ) {
        ApiSummary withName(String newName) {
            return new ApiSummary(
                    apiId,
                    newName,
                    protocol,
                    total,
                    success,
                    fail,
                    successRate,
                    samplesPerSecond,
                    durationStats,
                    sentMessages,
                    receivedMessages,
                    matchedMessages,
                    sendRate,
                    receiveRate,
                    matchedRate,
                    avgFirstMessageLatencyMs,
                    firstMessageLatencyStats,
                    topCompletionReason
            );
        }
    }

    public record DurationStats(
            long avg,
            long min,
            long max,
            long p90,
            long p95,
            long p99
    ) {
        public static DurationStats empty() {
            return new DurationStats(0, 0, 0, 0, 0, 0);
        }
    }
}

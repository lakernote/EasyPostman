package com.laker.postman.panel.performance.model;

import java.util.EnumMap;
import java.util.ArrayList;
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

    public PerformanceStatsSnapshot withLiveStreamMetrics(PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot) {
        if (liveSnapshot == null) {
            return this;
        }
        EnumMap<PerformanceProtocol, ApiSummary> mergedTotals = new EnumMap<>(PerformanceProtocol.class);
        mergedTotals.putAll(totalsByProtocol);
        List<ApiSummary> mergedSummaries = new ArrayList<>(summaries);
        mergeLiveSummaries(mergedSummaries, PerformanceProtocol.WEBSOCKET, liveSnapshot.webSocket());
        mergeLiveSummaries(mergedSummaries, PerformanceProtocol.SSE, liveSnapshot.sse());
        mergeLiveProtocol(mergedTotals, PerformanceProtocol.WEBSOCKET, liveSnapshot.webSocket());
        mergeLiveProtocol(mergedTotals, PerformanceProtocol.SSE, liveSnapshot.sse());

        long activeStreams = activeSessions(liveSnapshot.webSocket()) + activeSessions(liveSnapshot.sse());
        return new PerformanceStatsSnapshot(
                mergedSummaries,
                mergedTotals,
                totalRequests + activeStreams,
                successRequests + activeStreams,
                retainedRequestResultCount
        );
    }

    private static void mergeLiveSummaries(List<ApiSummary> summaries,
                                           PerformanceProtocol protocol,
                                           PerformanceRealtimeMetrics.LiveProtocolSnapshot live) {
        if (live == null || live.apiSnapshots() == null || live.apiSnapshots().isEmpty()) {
            return;
        }
        for (PerformanceRealtimeMetrics.LiveApiSnapshot liveApi : live.apiSnapshots()) {
            if (liveApi == null || liveApi.metrics() == null || !liveApi.metrics().hasData()) {
                continue;
            }
            ApiSummary liveSummary = liveSummary(
                    protocol,
                    liveApi.metrics(),
                    liveApi.apiId(),
                    resolveLiveApiName(liveApi)
            );
            int existingIndex = findSummaryIndex(summaries, protocol, liveSummary.apiId());
            if (existingIndex >= 0) {
                summaries.set(existingIndex, merge(summaries.get(existingIndex), liveApi.metrics()));
            } else {
                summaries.add(liveSummary);
            }
        }
    }

    private static int findSummaryIndex(List<ApiSummary> summaries, PerformanceProtocol protocol, String apiId) {
        for (int i = 0; i < summaries.size(); i++) {
            ApiSummary summary = summaries.get(i);
            String summaryApiId = summary.apiId() == null ? "" : summary.apiId();
            String targetApiId = apiId == null ? "" : apiId;
            if (summary.protocol() == protocol && summaryApiId.equals(targetApiId)) {
                return i;
            }
        }
        return -1;
    }

    private static String resolveLiveApiName(PerformanceRealtimeMetrics.LiveApiSnapshot liveApi) {
        if (liveApi.apiName() != null && !liveApi.apiName().isBlank()) {
            return liveApi.apiName();
        }
        return ApiMetadata.getName(liveApi.apiId());
    }

    private static void mergeLiveProtocol(EnumMap<PerformanceProtocol, ApiSummary> totals,
                                          PerformanceProtocol protocol,
                                          PerformanceRealtimeMetrics.LiveProtocolSnapshot live) {
        if (live == null || !live.hasData()) {
            return;
        }
        ApiSummary existing = totals.get(protocol);
        if (existing == null) {
            totals.put(protocol, liveSummary(protocol, live, "", ""));
            return;
        }
        totals.put(protocol, merge(existing, live));
    }

    private static ApiSummary liveSummary(PerformanceProtocol protocol,
                                          PerformanceRealtimeMetrics.LiveProtocolSnapshot live,
                                          String apiId,
                                          String apiName) {
        long total = live.activeSessions();
        return new ApiSummary(
                apiId == null ? "" : apiId,
                apiName == null ? "" : apiName,
                protocol,
                total,
                total,
                0,
                total > 0 ? 100.0 : 0,
                0,
                live.activeDurationStats(),
                live.sentMessages(),
                live.receivedMessages(),
                live.matchedMessages(),
                live.sendRate(),
                live.receiveRate(),
                live.matchedRate(),
                live.avgFirstMessageLatencyMs(),
                live.firstMessageLatencyStats(),
                "pending"
        );
    }

    private static ApiSummary merge(ApiSummary existing,
                                    PerformanceRealtimeMetrics.LiveProtocolSnapshot live) {
        long total = existing.total + live.activeSessions();
        long success = existing.success + live.activeSessions();
        long fail = existing.fail;
        boolean hasActiveLatency = live.firstMessageLatencyStats() != null
                && live.firstMessageLatencyStats().avg() > 0;
        return new ApiSummary(
                existing.apiId,
                existing.name,
                existing.protocol,
                total,
                success,
                fail,
                total > 0 ? success * 100.0 / total : 0,
                existing.samplesPerSecond,
                chooseLiveStats(existing.durationStats, live.activeDurationStats()),
                existing.sentMessages + live.sentMessages(),
                existing.receivedMessages + live.receivedMessages(),
                existing.matchedMessages + live.matchedMessages(),
                live.sendRate(),
                live.receiveRate(),
                live.matchedRate(),
                hasActiveLatency ? live.avgFirstMessageLatencyMs() : existing.avgFirstMessageLatencyMs,
                hasActiveLatency ? live.firstMessageLatencyStats() : existing.firstMessageLatencyStats,
                live.activeSessions() > 0 ? "pending" : existing.topCompletionReason
        );
    }

    private static DurationStats chooseLiveStats(DurationStats existing, DurationStats live) {
        if (live == null || live.avg() <= 0) {
            return existing;
        }
        return live;
    }

    private static int activeSessions(PerformanceRealtimeMetrics.LiveProtocolSnapshot live) {
        return live == null ? 0 : live.activeSessions();
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

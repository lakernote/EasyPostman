package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceReportSnapshot;
import com.laker.postman.performance.core.model.PerformanceStatsSnapshot;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class PerformanceJsonReportMapper {

    public PerformanceJsonReport fromStatsSnapshot(PerformanceJsonReportMetadata metadata,
                                                   PerformanceStatsSnapshot snapshot) {
        PerformanceStatsSnapshot safeSnapshot = snapshot;
        long total = safeSnapshot == null ? 0L : safeSnapshot.totalRequests();
        long success = safeSnapshot == null ? 0L : safeSnapshot.successRequests();
        return PerformanceJsonReport.builder()
                .metadata(metadata)
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(total)
                        .successRequests(success)
                        .build())
                .protocols(toProtocols(safeSnapshot))
                .build();
    }

    public PerformanceJsonReport fromReportSnapshot(PerformanceJsonReportMetadata metadata,
                                                    PerformanceReportSnapshot snapshot) {
        PerformanceReportSnapshot safeSnapshot = snapshot == null
                ? PerformanceReportSnapshot.of(null, null)
                : snapshot;
        PerformanceJsonReport completed = fromStatsSnapshot(metadata, safeSnapshot.completedStats());
        if (!hasLiveStreamData(safeSnapshot.liveSnapshot())) {
            return completed;
        }
        PerformanceJsonReport live = fromLiveSnapshot(metadata, safeSnapshot.liveSnapshot());
        PerformanceJsonReport merged = PerformanceJsonReportSummaryMapper.merge(
                "",
                "",
                "",
                "",
                List.of(completed, live)
        );
        return PerformanceJsonReport.builder()
                .metadata(metadata)
                .summary(completed.getSummary())
                .protocols(merged.getProtocols())
                .build();
    }

    private boolean hasLiveStreamData(PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot) {
        if (liveSnapshot == null) {
            return false;
        }
        return liveSnapshot.webSocket().hasData() || liveSnapshot.sse().hasData();
    }

    private Map<String, PerformanceJsonReportProtocol> toProtocols(PerformanceStatsSnapshot snapshot) {
        Map<String, PerformanceJsonReportProtocol> protocols = PerformanceJsonReportSummaryMapper.emptyProtocols();
        if (snapshot == null) {
            return protocols;
        }
        for (PerformanceProtocol protocol : PerformanceProtocol.values()) {
            List<PerformanceJsonReportApi> apis = new ArrayList<>();
            for (PerformanceStatsSnapshot.ApiSummary summary : snapshot.summaries()) {
                if (summary.protocol() == protocol) {
                    apis.add(toApi(summary));
                }
            }
            PerformanceStatsSnapshot.ApiSummary total = snapshot.totalFor(protocol, protocol.name() + " Total");
            protocols.put(protocol.name(), PerformanceJsonReportProtocol.builder()
                    .protocol(protocol.name())
                    .total(total == null ? emptyApi(protocol.name()) : toApi(total))
                    .apis(apis)
                    .build());
        }
        return protocols;
    }

    private PerformanceJsonReport fromLiveSnapshot(PerformanceJsonReportMetadata metadata,
                                                   PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot) {
        PerformanceRealtimeMetrics.LiveSnapshot safeLiveSnapshot = liveSnapshot == null
                ? PerformanceRealtimeMetrics.LiveSnapshot.empty()
                : liveSnapshot;
        Map<String, PerformanceJsonReportProtocol> protocols = PerformanceJsonReportSummaryMapper.emptyProtocols();
        putLiveProtocol(protocols, PerformanceProtocol.WEBSOCKET, safeLiveSnapshot.webSocket());
        putLiveProtocol(protocols, PerformanceProtocol.SSE, safeLiveSnapshot.sse());
        return PerformanceJsonReport.builder()
                .metadata(metadata)
                .summary(PerformanceJsonReportSummary.builder().build())
                .protocols(protocols)
                .build();
    }

    private void putLiveProtocol(Map<String, PerformanceJsonReportProtocol> protocols,
                                 PerformanceProtocol protocol,
                                 PerformanceRealtimeMetrics.LiveProtocolSnapshot snapshot) {
        if (protocol == null || snapshot == null || !snapshot.hasData()) {
            return;
        }
        List<PerformanceJsonReportApi> apis = new ArrayList<>();
        if (snapshot.apiSnapshots() != null) {
            for (PerformanceRealtimeMetrics.LiveApiSnapshot apiSnapshot : snapshot.apiSnapshots()) {
                if (apiSnapshot != null && apiSnapshot.metrics() != null && apiSnapshot.metrics().hasData()) {
                    apis.add(toLiveApi(protocol, apiSnapshot.apiId(), apiSnapshot.apiName(), apiSnapshot.metrics()));
                }
            }
        }
        protocols.put(protocol.name(), PerformanceJsonReportProtocol.builder()
                .protocol(protocol.name())
                .total(toLiveApi(protocol, "", protocol.name() + " Total", snapshot))
                .apis(apis)
                .build());
    }

    private PerformanceJsonReportApi emptyApi(String protocol) {
        return PerformanceJsonReportApi.builder().protocol(protocol).build();
    }

    private PerformanceJsonReportApi toApi(PerformanceStatsSnapshot.ApiSummary summary) {
        PerformanceStatsSnapshot.DurationStats durationStats = summary.durationStats() == null
                ? PerformanceStatsSnapshot.DurationStats.empty()
                : summary.durationStats();
        PerformanceStatsSnapshot.DurationStats firstLatencyStats = summary.firstMessageLatencyStats() == null
                ? PerformanceStatsSnapshot.DurationStats.empty()
                : summary.firstMessageLatencyStats();
        return PerformanceJsonReportApi.builder()
                .apiId(summary.apiId())
                .name(summary.name())
                .protocol(summary.protocol() == null ? "" : summary.protocol().name())
                .total(summary.total())
                .success(summary.success())
                .failed(summary.fail())
                .successRate(summary.successRate())
                .samplesPerSecond(summary.samplesPerSecond())
                .firstSampleStartTimeMs(summary.firstSampleStartTimeMs())
                .lastSampleEndTimeMs(summary.lastSampleEndTimeMs())
                .durationMs(toDuration(durationStats))
                .bytes(PerformanceJsonReportBytes.builder()
                        .sentBytes(summary.sentBytes())
                        .receivedBytes(summary.receivedBytes())
                        .sentBytesPerSecond(summary.sentBytesPerSecond())
                        .receivedBytesPerSecond(summary.receivedBytesPerSecond())
                        .avgReceivedBytes(summary.avgReceivedBytes())
                        .build())
                .stream(PerformanceJsonReportStream.builder()
                        .sentMessages(summary.sentMessages())
                        .receivedMessages(summary.receivedMessages())
                        .matchedMessages(summary.matchedMessages())
                        .sendRate(summary.sendRate())
                        .receiveRate(summary.receiveRate())
                        .matchedRate(summary.matchedRate())
                        .build())
                .firstMessageLatencyMs(toDuration(firstLatencyStats))
                .build();
    }

    private PerformanceJsonReportApi toLiveApi(PerformanceProtocol protocol,
                                               String apiId,
                                               String apiName,
                                               PerformanceRealtimeMetrics.LiveProtocolSnapshot snapshot) {
        int activeSessions = Math.max(0, snapshot == null ? 0 : snapshot.activeSessions());
        return PerformanceJsonReportApi.builder()
                .apiId(apiId)
                .name(apiName)
                .protocol(protocol == null ? "" : protocol.name())
                .total((long) activeSessions)
                .success((long) activeSessions)
                .failed(0L)
                .durationMs(toDuration(snapshot == null
                        ? PerformanceStatsSnapshot.DurationStats.empty()
                        : snapshot.activeDurationStats()))
                .stream(PerformanceJsonReportStream.builder()
                        .sentMessages(snapshot == null ? 0L : snapshot.sentMessages())
                        .receivedMessages(snapshot == null ? 0L : snapshot.receivedMessages())
                        .matchedMessages(snapshot == null ? 0L : snapshot.matchedMessages())
                        .sendRate(snapshot == null ? 0D : snapshot.sendRate())
                        .receiveRate(snapshot == null ? 0D : snapshot.receiveRate())
                        .matchedRate(snapshot == null ? 0D : snapshot.matchedRate())
                        .build())
                .firstMessageLatencyMs(toDuration(snapshot == null
                        ? PerformanceStatsSnapshot.DurationStats.empty()
                        : snapshot.firstMessageLatencyStats()))
                .build();
    }

    private PerformanceJsonReportDuration toDuration(PerformanceStatsSnapshot.DurationStats stats) {
        PerformanceStatsSnapshot.DurationStats safeStats = stats == null
                ? PerformanceStatsSnapshot.DurationStats.empty()
                : stats;
        return PerformanceJsonReportDuration.builder()
                .avg(safeStats.avg())
                .min(safeStats.min())
                .max(safeStats.max())
                .p90(safeStats.p90())
                .p95(safeStats.p95())
                .p99(safeStats.p99())
                .build();
    }
}

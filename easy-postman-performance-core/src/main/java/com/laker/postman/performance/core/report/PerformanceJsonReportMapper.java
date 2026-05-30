package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceStatsSnapshot;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Comparator;
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
            apis.sort(Comparator.comparing(PerformanceJsonReportApi::getName, String.CASE_INSENSITIVE_ORDER));
            PerformanceStatsSnapshot.ApiSummary total = snapshot.totalFor(protocol, protocol.name() + " Total");
            protocols.put(protocol.name(), PerformanceJsonReportProtocol.builder()
                    .protocol(protocol.name())
                    .total(total == null ? emptyApi(protocol.name()) : toApi(total))
                    .apis(apis)
                    .build());
        }
        return protocols;
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

    private PerformanceJsonReportDuration toDuration(PerformanceStatsSnapshot.DurationStats stats) {
        return PerformanceJsonReportDuration.builder()
                .avg(stats.avg())
                .min(stats.min())
                .max(stats.max())
                .p90(stats.p90())
                .p95(stats.p95())
                .p99(stats.p99())
                .build();
    }
}

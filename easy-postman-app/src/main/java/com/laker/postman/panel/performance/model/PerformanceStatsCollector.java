package com.laker.postman.panel.performance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PerformanceStatsCollector {

    private final EnumMap<PerformanceProtocol, Map<String, PerformanceSampleAccumulator>> apiStatsByProtocol =
            new EnumMap<>(PerformanceProtocol.class);
    private final EnumMap<PerformanceProtocol, PerformanceSampleAccumulator> protocolTotals = new EnumMap<>(PerformanceProtocol.class);
    private final PerformanceSampleAccumulator overallStats = new PerformanceSampleAccumulator("", PerformanceProtocol.HTTP);
    private final EnumMap<PerformanceProtocol, PerformanceSampleAccumulator> trendProtocolStats = new EnumMap<>(PerformanceProtocol.class);
    private PerformanceSampleAccumulator trendOverallStats = new PerformanceSampleAccumulator("", PerformanceProtocol.HTTP);
    private boolean trendEnabled = true;

    public synchronized void record(RequestResult result) {
        if (result == null) {
            return;
        }
        PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
        String apiId = result.apiId == null ? "" : result.apiId;
        Map<String, PerformanceSampleAccumulator> protocolApiStats =
                apiStatsByProtocol.computeIfAbsent(protocol, ignored -> new HashMap<>());

        protocolApiStats.computeIfAbsent(apiId, ignored -> new PerformanceSampleAccumulator(apiId, protocol)).record(result);
        protocolTotals.computeIfAbsent(protocol, ignored -> new PerformanceSampleAccumulator("", protocol)).record(result);
        overallStats.record(result);
        if (trendEnabled) {
            trendProtocolStats.computeIfAbsent(protocol, ignored -> new PerformanceSampleAccumulator("", protocol)).record(result);
            trendOverallStats.record(result);
        }
    }

    public synchronized void setTrendEnabled(boolean trendEnabled) {
        if (this.trendEnabled == trendEnabled) {
            return;
        }
        this.trendEnabled = trendEnabled;
        if (!trendEnabled) {
            clearTrendWindow();
        }
    }

    public synchronized PerformanceStatsSnapshot snapshot() {
        List<PerformanceStatsSnapshot.ApiSummary> summaries = new ArrayList<>();
        for (Map<String, PerformanceSampleAccumulator> statsByApi : apiStatsByProtocol.values()) {
            for (PerformanceSampleAccumulator stats : statsByApi.values()) {
                summaries.add(stats.toSummary(ApiMetadata.getName(stats.apiId())));
            }
        }

        EnumMap<PerformanceProtocol, PerformanceStatsSnapshot.ApiSummary> totals = new EnumMap<>(PerformanceProtocol.class);
        for (Map.Entry<PerformanceProtocol, PerformanceSampleAccumulator> entry : protocolTotals.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().toSummary(""));
        }

        return new PerformanceStatsSnapshot(
                summaries,
                totals,
                overallStats.total(),
                overallStats.success(),
                0
        );
    }

    public synchronized PerformanceTrendSnapshot sampleTrendSnapshot(long nowMs,
                                                                     int activeUsers,
                                                                     int activeWebSocketConnections,
                                                                     int activeSseStreams,
                                                                     long samplingIntervalMs,
                                                                     PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        PerformanceTrendSnapshot snapshot = new PerformanceTrendSnapshot(
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                toWindowMetrics(trendOverallStats, null, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(trendProtocolStats.get(PerformanceProtocol.HTTP),
                        PerformanceProtocol.HTTP, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(trendProtocolStats.get(PerformanceProtocol.WEBSOCKET),
                        PerformanceProtocol.WEBSOCKET, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(trendProtocolStats.get(PerformanceProtocol.SSE),
                        PerformanceProtocol.SSE, samplingIntervalMs, realtimeMetrics)
        );
        clearTrendWindow();
        return snapshot;
    }

    public synchronized void clear() {
        apiStatsByProtocol.clear();
        protocolTotals.clear();
        overallStats.clear();
        clearTrendWindow();
    }

    private void clearTrendWindow() {
        trendProtocolStats.clear();
        trendOverallStats = new PerformanceSampleAccumulator("", PerformanceProtocol.HTTP);
    }

    private static PerformanceTrendSnapshot.ProtocolWindowMetrics toWindowMetrics(
            PerformanceSampleAccumulator stats,
            PerformanceProtocol protocol,
            long samplingIntervalMs,
            PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        if (stats == null || stats.total() == 0) {
            return realtimeStreamMetrics(protocol, realtimeMetrics);
        }

        double seconds = Math.max(0.001, samplingIntervalMs / 1000.0);
        int samples = clampToInt(stats.total());
        int failures = clampToInt(stats.fail());
        double sentRate = round(stats.sentMessages() / seconds);
        double receivedRate = round(stats.receivedMessages() / seconds);
        double matchedRate = round(stats.matchedMessages() / seconds);
        double avgDuration = stats.avgDurationMs();
        double avgFirstMessageLatency = stats.avgFirstMessageLatencyMs();

        if (realtimeMetrics != null && protocol == PerformanceProtocol.WEBSOCKET) {
            sentRate = realtimeMetrics.webSocketSentRate();
            receivedRate = realtimeMetrics.webSocketReceivedRate();
            matchedRate = realtimeMetrics.webSocketMatchedRate();
            avgFirstMessageLatency = realtimeMetrics.webSocketFirstMessageLatencyMs();
            if (realtimeMetrics.webSocketActiveSessionDurationMs() > 0) {
                avgDuration = realtimeMetrics.webSocketActiveSessionDurationMs();
            }
        } else if (realtimeMetrics != null && protocol == PerformanceProtocol.SSE) {
            receivedRate = realtimeMetrics.sseReceivedRate();
            matchedRate = realtimeMetrics.sseMatchedRate();
            avgFirstMessageLatency = realtimeMetrics.sseFirstMessageLatencyMs();
            if (realtimeMetrics.sseActiveSessionDurationMs() > 0) {
                avgDuration = realtimeMetrics.sseActiveSessionDurationMs();
            }
        }

        return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                samples,
                failures,
                stats.total() > 0 ? round((double) failures * 100 / stats.total()) : 0,
                round(stats.total() / seconds),
                avgDuration,
                clampToInt(stats.sentMessages()),
                clampToInt(stats.receivedMessages()),
                clampToInt(stats.matchedMessages()),
                sentRate,
                receivedRate,
                matchedRate,
                avgFirstMessageLatency
        );
    }

    private static PerformanceTrendSnapshot.ProtocolWindowMetrics realtimeStreamMetrics(
            PerformanceProtocol protocol,
            PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        if (realtimeMetrics != null && protocol == PerformanceProtocol.WEBSOCKET) {
            return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                    0,
                    0,
                    0,
                    0,
                    realtimeMetrics.webSocketActiveSessionDurationMs(),
                    0,
                    0,
                    0,
                    realtimeMetrics.webSocketSentRate(),
                    realtimeMetrics.webSocketReceivedRate(),
                    realtimeMetrics.webSocketMatchedRate(),
                    realtimeMetrics.webSocketFirstMessageLatencyMs()
            );
        }
        if (realtimeMetrics != null && protocol == PerformanceProtocol.SSE) {
            return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                    0,
                    0,
                    0,
                    0,
                    realtimeMetrics.sseActiveSessionDurationMs(),
                    0,
                    0,
                    0,
                    0,
                    realtimeMetrics.sseReceivedRate(),
                    realtimeMetrics.sseMatchedRate(),
                    realtimeMetrics.sseFirstMessageLatencyMs()
            );
        }
        return new PerformanceTrendSnapshot.ProtocolWindowMetrics(0, 0, 0, 0, Double.NaN, 0, 0, 0, 0, 0, 0, Double.NaN);
    }

    private static int clampToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, value);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

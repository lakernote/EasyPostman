package com.laker.postman.performance.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;

public final class PerformanceTrendWindowCollector {

    private final EnumMap<PerformanceProtocol, PerformanceSampleAccumulator> protocolStats =
            new EnumMap<>(PerformanceProtocol.class);
    private PerformanceSampleAccumulator overallStats = new PerformanceSampleAccumulator("", PerformanceProtocol.HTTP);
    private boolean enabled = true;

    public synchronized void record(RequestResult result) {
        if (!enabled || result == null) {
            return;
        }
        PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
        protocolStats.computeIfAbsent(protocol, ignored -> new PerformanceSampleAccumulator("", protocol)).record(result);
        overallStats.record(result);
    }

    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    public synchronized PerformanceTrendSnapshot sampleSnapshot(int activeUsers,
                                                               int activeWebSocketConnections,
                                                               int activeSseStreams,
                                                               long samplingIntervalMs,
                                                               PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        PerformanceTrendSnapshot snapshot = new PerformanceTrendSnapshot(
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                toWindowMetrics(overallStats, null, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(protocolStats.get(PerformanceProtocol.HTTP),
                        PerformanceProtocol.HTTP, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(protocolStats.get(PerformanceProtocol.WEBSOCKET),
                        PerformanceProtocol.WEBSOCKET, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(protocolStats.get(PerformanceProtocol.SSE),
                        PerformanceProtocol.SSE, samplingIntervalMs, realtimeMetrics)
        );
        clear();
        return snapshot;
    }

    public synchronized void clear() {
        protocolStats.clear();
        overallStats = new PerformanceSampleAccumulator("", PerformanceProtocol.HTTP);
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

package com.laker.postman.performance.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record PerformanceTrendSnapshot(
        int activeUsers,
        int activeWebSocketConnections,
        int activeSseStreams,
        ProtocolWindowMetrics overview,
        ProtocolWindowMetrics http,
        ProtocolWindowMetrics webSocket,
        ProtocolWindowMetrics sse
) {

    public static PerformanceTrendSnapshot fromResults(List<RequestResult> results,
                                                       long windowStart,
                                                       long now,
                                                       int activeUsers,
                                                       int activeWebSocketConnections,
                                                       int activeSseStreams,
                                                       long samplingIntervalMs) {
        return fromResults(
                results,
                windowStart,
                now,
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                samplingIntervalMs,
                null
        );
    }

    public static PerformanceTrendSnapshot fromResults(List<RequestResult> results,
                                                       long windowStart,
                                                       long now,
                                                       int activeUsers,
                                                       int activeWebSocketConnections,
                                                       int activeSseStreams,
                                                       long samplingIntervalMs,
                                                       PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        List<RequestResult> safeResults = results == null ? List.of() : results;
        List<RequestResult> windowResults = safeResults.stream()
                .filter(result -> result.endTime >= windowStart && result.endTime <= now)
                .toList();
        return new PerformanceTrendSnapshot(
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                calculate(windowResults, null, samplingIntervalMs, null),
                calculate(windowResults, PerformanceProtocol.HTTP, samplingIntervalMs, null),
                calculate(windowResults, PerformanceProtocol.WEBSOCKET, samplingIntervalMs, realtimeMetrics),
                calculate(windowResults, PerformanceProtocol.SSE, samplingIntervalMs, realtimeMetrics)
        );
    }

    private static ProtocolWindowMetrics calculate(List<RequestResult> results,
                                                   PerformanceProtocol protocol,
                                                   long samplingIntervalMs,
                                                   PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        List<RequestResult> filtered = results.stream()
                .filter(result -> protocol == null || result.protocol == protocol)
                .toList();
        int samples = filtered.size();
        int failures = 0;
        long totalDuration = 0;
        long firstStart = Long.MAX_VALUE;
        long firstEnd = Long.MAX_VALUE;
        long lastEnd = 0;
        int sentMessages = 0;
        int receivedMessages = 0;
        int matchedMessages = 0;
        long firstMessageLatencyTotal = 0;
        int firstMessageLatencyCount = 0;

        for (RequestResult result : filtered) {
            if (!result.success) {
                failures++;
            }
            totalDuration += result.getResponseTime();
            firstStart = Math.min(firstStart, result.startTime);
            firstEnd = Math.min(firstEnd, result.endTime);
            lastEnd = Math.max(lastEnd, result.endTime);
            sentMessages += Math.max(0, result.sentMessages);
            receivedMessages += Math.max(0, result.receivedMessages);
            matchedMessages += Math.max(0, result.matchedMessages);
            if (result.firstMessageLatencyMs >= 0) {
                firstMessageLatencyTotal += result.firstMessageLatencyMs;
                firstMessageLatencyCount++;
            }
        }

        double sampleSeconds = resolveEndWindowSeconds(samples, firstEnd, lastEnd, samplingIntervalMs);
        double streamSeconds = resolveSessionWindowSeconds(samples, firstStart, lastEnd, samplingIntervalMs);
        double sampleRate = sampleSeconds > 0 ? round(samples / sampleSeconds) : 0;
        double sentRate = streamSeconds > 0 ? round(sentMessages / streamSeconds) : 0;
        double receivedRate = streamSeconds > 0 ? round(receivedMessages / streamSeconds) : 0;
        double matchedRate = streamSeconds > 0 ? round(matchedMessages / streamSeconds) : 0;
        double avgDuration = samples > 0 ? round((double) totalDuration / samples) : Double.NaN;
        double failurePercent = samples > 0 ? round((double) failures * 100 / samples) : 0;
        double avgFirstMessageLatency = firstMessageLatencyCount > 0
                ? round((double) firstMessageLatencyTotal / firstMessageLatencyCount)
                : Double.NaN;
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

        return new ProtocolWindowMetrics(
                samples,
                failures,
                failurePercent,
                sampleRate,
                avgDuration,
                sentMessages,
                receivedMessages,
                matchedMessages,
                sentRate,
                receivedRate,
                matchedRate,
                avgFirstMessageLatency
        );
    }

    private static double resolveEndWindowSeconds(int samples,
                                                  long firstEnd,
                                                  long lastEnd,
                                                  long samplingIntervalMs) {
        if (samples == 0) {
            return 0;
        }
        if (firstEnd != Long.MAX_VALUE && lastEnd > firstEnd) {
            return Math.max(0.001, (lastEnd - firstEnd) / 1000.0);
        }
        return Math.max(0.001, samplingIntervalMs / 1000.0);
    }

    private static double resolveSessionWindowSeconds(int samples,
                                                      long firstStart,
                                                      long lastEnd,
                                                      long samplingIntervalMs) {
        if (samples == 0) {
            return 0;
        }
        if (firstStart != Long.MAX_VALUE && lastEnd > firstStart) {
            return Math.max(0.001, (lastEnd - firstStart) / 1000.0);
        }
        return Math.max(0.001, samplingIntervalMs / 1000.0);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record ProtocolWindowMetrics(
            int samples,
            int failures,
            double failurePercent,
            double sampleRate,
            double avgDurationMs,
            int sentMessages,
            int receivedMessages,
            int matchedMessages,
            double sentRate,
            double receivedRate,
            double matchedRate,
            double avgFirstMessageLatencyMs
    ) {
    }
}

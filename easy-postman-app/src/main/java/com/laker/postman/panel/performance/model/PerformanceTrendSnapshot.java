package com.laker.postman.panel.performance.model;

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
        List<RequestResult> safeResults = results == null ? List.of() : results;
        List<RequestResult> windowResults = safeResults.stream()
                .filter(result -> result.endTime >= windowStart && result.endTime <= now)
                .toList();
        return new PerformanceTrendSnapshot(
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                calculate(windowResults, null, samplingIntervalMs),
                calculate(windowResults, PerformanceProtocol.HTTP, samplingIntervalMs),
                calculate(windowResults, PerformanceProtocol.WEBSOCKET, samplingIntervalMs),
                calculate(windowResults, PerformanceProtocol.SSE, samplingIntervalMs)
        );
    }

    private static ProtocolWindowMetrics calculate(List<RequestResult> results,
                                                   PerformanceProtocol protocol,
                                                   long samplingIntervalMs) {
        List<RequestResult> filtered = results.stream()
                .filter(result -> protocol == null || result.protocol == protocol)
                .toList();
        int samples = filtered.size();
        int failures = 0;
        long totalDuration = 0;
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
            sentMessages += Math.max(0, result.sentMessages);
            receivedMessages += Math.max(0, result.receivedMessages);
            matchedMessages += Math.max(0, result.matchedMessages);
            if (result.firstMessageLatencyMs >= 0) {
                firstMessageLatencyTotal += result.firstMessageLatencyMs;
                firstMessageLatencyCount++;
            }
        }

        double seconds = resolveWindowSeconds(samples, samplingIntervalMs);
        double sampleRate = seconds > 0 ? round(samples / seconds) : 0;
        double sentRate = seconds > 0 ? round(sentMessages / seconds) : 0;
        double receivedRate = seconds > 0 ? round(receivedMessages / seconds) : 0;
        double matchedRate = seconds > 0 ? round(matchedMessages / seconds) : 0;
        double avgDuration = samples > 0 ? round((double) totalDuration / samples) : 0;
        double failurePercent = samples > 0 ? round((double) failures * 100 / samples) : 0;
        double avgFirstMessageLatency = firstMessageLatencyCount > 0
                ? round((double) firstMessageLatencyTotal / firstMessageLatencyCount)
                : 0;

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

    private static double resolveWindowSeconds(int samples, long samplingIntervalMs) {
        if (samples == 0) {
            return 0;
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

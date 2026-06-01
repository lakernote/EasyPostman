package com.laker.postman.performance.result;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceReportSnapshot;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;


import lombok.RequiredArgsConstructor;

import java.util.function.IntSupplier;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;

@RequiredArgsConstructor
public final class PerformanceMetricsSnapshotService {

    private static final long DEFAULT_SAMPLING_INTERVAL_MS = 1000L;

    private final PerformanceStatsCollector statsCollector;
    private final PerformanceTrendWindowCollector trendWindowCollector;
    private final IntSupplier activeThreadsSupplier;
    private final IntSupplier activeWebSocketsSupplier;
    private final IntSupplier activeSseStreamsSupplier;
    private final LongSupplier samplingIntervalSupplier;
    private final LongFunction<PerformanceRealtimeMetrics.Sample> realtimeMetricsSampler;
    private final LongFunction<PerformanceRealtimeMetrics.LiveSnapshot> liveMetricsSnapshotSupplier;

    public PerformanceReportSnapshot reportSnapshot(long nowMs) {
        PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot = liveMetricsSnapshotSupplier == null
                ? PerformanceRealtimeMetrics.LiveSnapshot.empty()
                : liveMetricsSnapshotSupplier.apply(nowMs);
        return PerformanceReportSnapshot.of(statsCollector().snapshot(), liveSnapshot);
    }

    public PerformanceTrendSnapshot trendSnapshot(long nowMs) {
        PerformanceRealtimeMetrics.Sample realtimeMetrics = sampleRealtimeMetrics(nowMs);
        return trendWindowCollector().sampleSnapshot(
                activeUsers(),
                activeWebSocketSessions(realtimeMetrics),
                activeSseStreams(realtimeMetrics),
                samplingIntervalMs(),
                realtimeMetrics
        );
    }

    private PerformanceStatsCollector statsCollector() {
        return statsCollector == null ? new PerformanceStatsCollector() : statsCollector;
    }

    private PerformanceTrendWindowCollector trendWindowCollector() {
        return trendWindowCollector == null ? new PerformanceTrendWindowCollector() : trendWindowCollector;
    }

    private PerformanceRealtimeMetrics.Sample sampleRealtimeMetrics(long nowMs) {
        PerformanceRealtimeMetrics.Sample sample = realtimeMetricsSampler == null
                ? PerformanceRealtimeMetrics.Sample.empty()
                : realtimeMetricsSampler.apply(nowMs);
        return sample == null ? PerformanceRealtimeMetrics.Sample.empty() : sample;
    }

    private int activeUsers() {
        return activeThreadsSupplier == null ? 0 : activeThreadsSupplier.getAsInt();
    }

    private int activeWebSocketSessions(PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        int activeWebSockets = activeWebSocketsSupplier == null ? 0 : activeWebSocketsSupplier.getAsInt();
        return Math.max(activeWebSockets, realtimeMetrics.webSocketActiveSessions());
    }

    private int activeSseStreams(PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        int activeSseStreams = activeSseStreamsSupplier == null ? 0 : activeSseStreamsSupplier.getAsInt();
        return Math.max(activeSseStreams, realtimeMetrics.sseActiveSessions());
    }

    private long samplingIntervalMs() {
        return samplingIntervalSupplier == null ? DEFAULT_SAMPLING_INTERVAL_MS : samplingIntervalSupplier.getAsLong();
    }
}

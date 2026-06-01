package com.laker.postman.performance.result;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceReportSnapshot;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceStatsSnapshot;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;


import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicLong lastTrendDrainAtMs = new AtomicLong(-1L);

    public PerformanceReportSnapshot reportSnapshot(long nowMs) {
        PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot = liveMetricsSnapshotSupplier == null
                ? PerformanceRealtimeMetrics.LiveSnapshot.empty()
                : liveMetricsSnapshotSupplier.apply(nowMs);
        return PerformanceReportSnapshot.of(statsCollector().snapshot(), liveSnapshot);
    }

    public PerformanceStatsSnapshot statsSnapshot() {
        return statsCollector().snapshot();
    }

    public PerformanceTrendSnapshot drainTrendWindowSnapshot(long nowMs) {
        PerformanceRealtimeMetrics.Sample realtimeMetrics = drainRealtimeMetricsWindow(nowMs);
        return trendWindowCollector().drainWindowSnapshot(
                activeUsers(),
                activeWebSocketSessions(realtimeMetrics),
                activeSseStreams(realtimeMetrics),
                trendWindowElapsedMs(nowMs),
                realtimeMetrics
        );
    }

    public void resetTrendWindow(long startTimeMs) {
        lastTrendDrainAtMs.set(Math.max(0L, startTimeMs));
    }

    private PerformanceStatsCollector statsCollector() {
        return statsCollector == null ? new PerformanceStatsCollector() : statsCollector;
    }

    private PerformanceTrendWindowCollector trendWindowCollector() {
        return trendWindowCollector == null ? new PerformanceTrendWindowCollector() : trendWindowCollector;
    }

    private PerformanceRealtimeMetrics.Sample drainRealtimeMetricsWindow(long nowMs) {
        if (realtimeMetricsSampler == null) {
            return null;
        }
        return realtimeMetricsSampler.apply(nowMs);
    }

    private int activeUsers() {
        return activeThreadsSupplier == null ? 0 : activeThreadsSupplier.getAsInt();
    }

    private int activeWebSocketSessions(PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        int activeWebSockets = activeWebSocketsSupplier == null ? 0 : activeWebSocketsSupplier.getAsInt();
        int realtimeActive = realtimeMetrics == null ? 0 : realtimeMetrics.webSocketActiveSessions();
        return Math.max(activeWebSockets, realtimeActive);
    }

    private int activeSseStreams(PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        int activeSseStreams = activeSseStreamsSupplier == null ? 0 : activeSseStreamsSupplier.getAsInt();
        int realtimeActive = realtimeMetrics == null ? 0 : realtimeMetrics.sseActiveSessions();
        return Math.max(activeSseStreams, realtimeActive);
    }

    private long samplingIntervalMs() {
        return samplingIntervalSupplier == null ? DEFAULT_SAMPLING_INTERVAL_MS : samplingIntervalSupplier.getAsLong();
    }

    private long trendWindowElapsedMs(long nowMs) {
        long previous = lastTrendDrainAtMs.getAndSet(Math.max(0L, nowMs));
        if (previous >= 0L && nowMs > previous) {
            return Math.max(1L, nowMs - previous);
        }
        return samplingIntervalMs();
    }
}

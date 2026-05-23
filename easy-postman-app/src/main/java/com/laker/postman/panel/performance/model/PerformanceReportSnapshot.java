package com.laker.postman.panel.performance.model;

/**
 * Report-time view that keeps completed samples separate from live stream state.
 * Completed stats are the final source of truth; live metrics are a pending overlay.
 */
public record PerformanceReportSnapshot(
        PerformanceStatsSnapshot completedStats,
        PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot
) {
    public PerformanceReportSnapshot {
        if (completedStats == null) {
            completedStats = new PerformanceStatsCollector().snapshot();
        }
        if (liveSnapshot == null) {
            liveSnapshot = PerformanceRealtimeMetrics.LiveSnapshot.empty();
        }
    }

    public static PerformanceReportSnapshot of(PerformanceStatsSnapshot completedStats,
                                               PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot) {
        return new PerformanceReportSnapshot(completedStats, liveSnapshot);
    }
}

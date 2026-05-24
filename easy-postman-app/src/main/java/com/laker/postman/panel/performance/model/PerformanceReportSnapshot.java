package com.laker.postman.panel.performance.model;

/**
 * Report-time view that keeps completed samples separate from live stream state.
 * Report builders merge live stream metrics only while sessions are still active.
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

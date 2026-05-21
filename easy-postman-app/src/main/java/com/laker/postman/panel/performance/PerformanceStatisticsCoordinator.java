package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.model.PerformanceStatsSnapshot;
import com.laker.postman.panel.performance.model.PerformanceTrendSnapshot;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;

import javax.swing.*;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;

@Slf4j
@RequiredArgsConstructor
final class PerformanceStatisticsCoordinator {

    private final PerformanceStatsCollector statsCollector;
    private final PerformanceReportPanel performanceReportPanel;
    private final PerformanceTrendPanel performanceTrendPanel;
    private final JTabbedPane resultTabbedPane;
    private final IntSupplier activeThreadsSupplier;
    private final IntSupplier activeWebSocketsSupplier;
    private final IntSupplier activeSseStreamsSupplier;
    private final LongSupplier samplingIntervalSupplier;
    private final LongFunction<PerformanceRealtimeMetrics.Sample> realtimeMetricsSampler;

    void refreshReport() {
        try {
            if (resultTabbedPane.getSelectedIndex() != 1) {
                log.debug("当前未查看报表Tab，跳过刷新");
                return;
            }
            updateReportWithLatestData();
        } catch (Exception ex) {
            log.warn("实时刷新报表失败: {}", ex.getMessage(), ex);
        }
    }

    void updateReportWithLatestData() {
        CompletableFuture.runAsync(() -> {
            PerformanceStatsSnapshot snapshot = statsCollector.snapshot();
            SwingUtilities.invokeLater(() -> performanceReportPanel.updateReport(snapshot));
        }).exceptionally(ex -> {
            log.error("报表更新失败", ex);
            return null;
        });
    }

    void updateReportWithLatestDataSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateReportWithLatestDataSync);
            return;
        }

        performanceReportPanel.updateReport(statsCollector.snapshot());
    }

    void sampleTrendData() {
        int users = activeThreadsSupplier.getAsInt();
        int activeWebSockets = activeWebSocketsSupplier.getAsInt();
        int activeSseStreams = activeSseStreamsSupplier.getAsInt();
        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        long samplingIntervalMs = samplingIntervalSupplier.getAsLong();
        PerformanceRealtimeMetrics.Sample realtimeMetrics = sampleRealtimeMetrics(now);

        CompletableFuture.runAsync(() -> {
            PerformanceTrendSnapshot snapshot = statsCollector.sampleTrendSnapshot(
                    now,
                    users,
                    activeWebSockets,
                    activeSseStreams,
                    samplingIntervalMs,
                    realtimeMetrics
            );
            SwingUtilities.invokeLater(() -> {
                log.debug("采样数据 {} - 用户数: {}, HTTP: {}, WS: {}, SSE: {}",
                        period, users, snapshot.http().samples(), snapshot.webSocket().samples(), snapshot.sse().samples());
                performanceTrendPanel.addOrUpdate(period, snapshot);
            });
        }).exceptionally(ex -> {
            log.error("趋势图采样失败", ex);
            return null;
        });
    }

    void sampleTrendDataSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::sampleTrendDataSync);
            return;
        }

        int users = activeThreadsSupplier.getAsInt();
        int activeWebSockets = activeWebSocketsSupplier.getAsInt();
        int activeSseStreams = activeSseStreamsSupplier.getAsInt();
        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        long samplingIntervalMs = samplingIntervalSupplier.getAsLong();
        PerformanceRealtimeMetrics.Sample realtimeMetrics = sampleRealtimeMetrics(now);
        PerformanceTrendSnapshot snapshot = statsCollector.sampleTrendSnapshot(
                now,
                users,
                activeWebSockets,
                activeSseStreams,
                samplingIntervalMs,
                realtimeMetrics
        );

        log.debug("同步采样数据 {} - 用户数: {}, HTTP: {}, WS: {}, SSE: {}",
                period, users, snapshot.http().samples(), snapshot.webSocket().samples(), snapshot.sse().samples());
        performanceTrendPanel.addOrUpdate(period, snapshot);
    }

    private PerformanceRealtimeMetrics.Sample sampleRealtimeMetrics(long nowMs) {
        return realtimeMetricsSampler == null
                ? PerformanceRealtimeMetrics.Sample.empty()
                : realtimeMetricsSampler.apply(nowMs);
    }

    private static RegularTimePeriod createTrendPeriod(long timestampMs) {
        return new Millisecond(new Date(timestampMs));
    }

}

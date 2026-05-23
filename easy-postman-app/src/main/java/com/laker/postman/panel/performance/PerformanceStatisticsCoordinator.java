package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceReportSnapshot;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
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
    private final BooleanSupplier trendEnabledSupplier;
    private final LongFunction<PerformanceRealtimeMetrics.Sample> realtimeMetricsSampler;
    private final LongFunction<PerformanceRealtimeMetrics.LiveSnapshot> liveMetricsSnapshotSupplier;
    private final ExecutorService metricsExecutor =
            Executors.newSingleThreadExecutor(PerformanceThreadFactory.daemonFactory("PerformanceMetrics"));
    private final AtomicLong runGeneration = new AtomicLong();
    private final Object runGenerationLock = new Object();
    private volatile boolean disposed;

    void refreshReport() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshReport);
            return;
        }

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
        long generation = currentGeneration();
        submitMetricsTask("报表更新", generation, () -> {
            PerformanceReportSnapshot snapshot = snapshotForReport(System.currentTimeMillis());
            invokeUiIfActive(generation, () -> performanceReportPanel.updateReport(snapshot));
        });
    }

    void updateReportWithLatestDataSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateReportWithLatestDataSync);
            return;
        }

        performanceReportPanel.updateReport(snapshotForReport(System.currentTimeMillis()));
    }

    void sampleTrendData() {
        if (!isTrendEnabled()) {
            log.debug("趋势图未启用，跳过采样");
            return;
        }
        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        long samplingIntervalMs = samplingIntervalSupplier.getAsLong();
        PerformanceRealtimeMetrics.Sample realtimeMetrics = sampleRealtimeMetrics(now);
        int users = activeThreadsSupplier.getAsInt();
        int activeWebSockets = resolveActiveWebSocketSessions(realtimeMetrics);
        int activeSseStreams = resolveActiveSseStreams(realtimeMetrics);
        long generation = currentGeneration();

        submitMetricsTask("趋势图采样", generation, () -> {
            PerformanceTrendSnapshot snapshot = statsCollector.sampleTrendSnapshot(
                    now,
                    users,
                    activeWebSockets,
                    activeSseStreams,
                    samplingIntervalMs,
                    realtimeMetrics
            );
            invokeUiIfActive(generation, () -> {
                log.debug("采样数据 {} - 用户数: {}, HTTP: {}, WS: {}, SSE: {}",
                        period, users, snapshot.http().samples(), snapshot.webSocket().samples(), snapshot.sse().samples());
                performanceTrendPanel.addOrUpdate(period, snapshot);
            });
        });
    }

    void sampleTrendDataSync() {
        if (!isTrendEnabled()) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::sampleTrendDataSync);
            return;
        }

        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        long samplingIntervalMs = samplingIntervalSupplier.getAsLong();
        PerformanceRealtimeMetrics.Sample realtimeMetrics = sampleRealtimeMetrics(now);
        int users = activeThreadsSupplier.getAsInt();
        int activeWebSockets = resolveActiveWebSocketSessions(realtimeMetrics);
        int activeSseStreams = resolveActiveSseStreams(realtimeMetrics);
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
        PerformanceRealtimeMetrics.Sample sample = realtimeMetricsSampler == null
                ? PerformanceRealtimeMetrics.Sample.empty()
                : realtimeMetricsSampler.apply(nowMs);
        return sample == null ? PerformanceRealtimeMetrics.Sample.empty() : sample;
    }

    private int resolveActiveWebSocketSessions(PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        int activeWebSockets = activeWebSocketsSupplier.getAsInt();
        return Math.max(activeWebSockets, realtimeMetrics.webSocketActiveSessions());
    }

    private int resolveActiveSseStreams(PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        int activeSseStreams = activeSseStreamsSupplier.getAsInt();
        return Math.max(activeSseStreams, realtimeMetrics.sseActiveSessions());
    }

    private PerformanceReportSnapshot snapshotForReport(long nowMs) {
        PerformanceStatsSnapshot snapshot = statsCollector == null
                ? new PerformanceStatsCollector().snapshot()
                : statsCollector.snapshot();
        PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot = liveMetricsSnapshotSupplier == null
                ? PerformanceRealtimeMetrics.LiveSnapshot.empty()
                : liveMetricsSnapshotSupplier.apply(nowMs);
        return PerformanceReportSnapshot.of(snapshot, liveSnapshot);
    }

    private boolean isTrendEnabled() {
        return trendEnabledSupplier == null || trendEnabledSupplier.getAsBoolean();
    }

    void dispose() {
        disposed = true;
        resetForNewRun();
        metricsExecutor.shutdownNow();
    }

    void resetForNewRun() {
        synchronized (runGenerationLock) {
            runGeneration.incrementAndGet();
        }
    }

    private long currentGeneration() {
        return runGeneration.get();
    }

    private boolean isCurrentGeneration(long generation) {
        return generation == currentGeneration();
    }

    private void submitMetricsTask(String taskName, long generation, Runnable task) {
        if (disposed) {
            return;
        }
        try {
            metricsExecutor.execute(() -> {
                synchronized (runGenerationLock) {
                    if (!isCurrentGeneration(generation)) {
                        log.debug("{}已过期，跳过旧压测轮次的异步任务", taskName);
                        return;
                    }
                    try {
                        task.run();
                    } catch (Exception ex) {
                        log.error("{}失败", taskName, ex);
                    }
                }
            });
        } catch (RejectedExecutionException ex) {
            if (!disposed) {
                log.warn("{}提交失败", taskName, ex);
            }
        }
    }

    private void invokeUiIfActive(long generation, Runnable uiTask) {
        SwingUtilities.invokeLater(() -> {
            if (!disposed && isCurrentGeneration(generation)) {
                uiTask.run();
            }
        });
    }

    private static RegularTimePeriod createTrendPeriod(long timestampMs) {
        return new Millisecond(new Date(timestampMs));
    }

}

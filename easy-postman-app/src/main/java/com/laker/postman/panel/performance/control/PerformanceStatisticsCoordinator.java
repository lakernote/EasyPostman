package com.laker.postman.panel.performance.control;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;
import com.laker.postman.performance.core.runtime.PerformanceThreadFactory;


import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceTrendView;
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
public final class PerformanceStatisticsCoordinator {

    private final PerformanceReportPanel performanceReportPanel;
    private final PerformanceTrendView performanceTrendPanel;
    private final JTabbedPane resultTabbedPane;
    private final BooleanSupplier trendEnabledSupplier;
    private final PerformanceMetricsSnapshotService snapshotService;
    private final ExecutorService metricsExecutor =
            Executors.newSingleThreadExecutor(PerformanceThreadFactory.daemonFactory("PerformanceMetrics"));
    private final AtomicLong runGeneration = new AtomicLong();
    private final Object runGenerationLock = new Object();
    private volatile boolean disposed;

    public PerformanceStatisticsCoordinator(PerformanceStatsCollector statsCollector,
                                            PerformanceTrendWindowCollector trendWindowCollector,
                                            PerformanceReportPanel performanceReportPanel,
                                            PerformanceTrendView performanceTrendPanel,
                                            JTabbedPane resultTabbedPane,
                                            IntSupplier activeThreadsSupplier,
                                            IntSupplier activeWebSocketsSupplier,
                                            IntSupplier activeSseStreamsSupplier,
                                            LongSupplier samplingIntervalSupplier,
                                            BooleanSupplier trendEnabledSupplier,
                                            LongFunction<PerformanceRealtimeMetrics.Sample> realtimeMetricsSampler,
                                            LongFunction<PerformanceRealtimeMetrics.LiveSnapshot> liveMetricsSnapshotSupplier) {
        this.performanceReportPanel = performanceReportPanel;
        this.performanceTrendPanel = performanceTrendPanel;
        this.resultTabbedPane = resultTabbedPane;
        this.trendEnabledSupplier = trendEnabledSupplier;
        this.snapshotService = new PerformanceMetricsSnapshotService(
                statsCollector,
                trendWindowCollector,
                activeThreadsSupplier,
                activeWebSocketsSupplier,
                activeSseStreamsSupplier,
                samplingIntervalSupplier,
                realtimeMetricsSampler,
                liveMetricsSnapshotSupplier
        );
    }

    public void refreshReport() {
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

    public void updateReportWithLatestData() {
        long generation = currentGeneration();
        submitMetricsTask("报表更新", generation, () -> {
            var snapshot = snapshotService.reportSnapshot(System.currentTimeMillis());
            invokeUiIfActive(generation, () -> performanceReportPanel.updateReport(snapshot));
        });
    }

    public void updateReportWithLatestDataSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateReportWithLatestDataSync);
            return;
        }

        performanceReportPanel.updateReport(snapshotService.reportSnapshot(System.currentTimeMillis()));
    }

    public void sampleTrendData() {
        if (!isTrendEnabled()) {
            log.debug("趋势图未启用，跳过采样");
            return;
        }
        if (performanceTrendPanel == null) {
            log.debug("趋势图面板未初始化，跳过采样");
            return;
        }
        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        long generation = currentGeneration();

        submitMetricsTask("趋势图采样", generation, () -> {
            PerformanceTrendSnapshot snapshot = snapshotService.trendSnapshot(now);
            invokeUiIfActive(generation, () -> {
                log.debug("采样数据 {} - 用户数: {}, HTTP: {}, WS: {}, SSE: {}",
                        period, snapshot.activeUsers(), snapshot.http().samples(), snapshot.webSocket().samples(), snapshot.sse().samples());
                performanceTrendPanel.addOrUpdate(period, snapshot);
            });
        });
    }

    public void sampleTrendDataSync() {
        if (!isTrendEnabled()) {
            return;
        }
        if (performanceTrendPanel == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::sampleTrendDataSync);
            return;
        }

        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        PerformanceTrendSnapshot snapshot = snapshotService.trendSnapshot(now);

        log.debug("同步采样数据 {} - 用户数: {}, HTTP: {}, WS: {}, SSE: {}",
                period, snapshot.activeUsers(), snapshot.http().samples(), snapshot.webSocket().samples(), snapshot.sse().samples());
        performanceTrendPanel.addOrUpdate(period, snapshot);
    }

    private boolean isTrendEnabled() {
        return trendEnabledSupplier == null || trendEnabledSupplier.getAsBoolean();
    }

    public void dispose() {
        disposed = true;
        resetForNewRun();
        metricsExecutor.shutdownNow();
    }

    public void resetForNewRun() {
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

package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceTrendSnapshot;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;

@Slf4j
@RequiredArgsConstructor
final class PerformanceStatisticsCoordinator {

    private final Object statsLock;
    private final List<RequestResult> allRequestResults;
    private final Map<String, List<Long>> apiCostMap;
    private final Map<String, Integer> apiSuccessMap;
    private final Map<String, Integer> apiFailMap;
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
            StatsSnapshot snapshot = copySnapshot();
            SwingUtilities.invokeLater(() -> performanceReportPanel.updateReport(
                    snapshot.apiCostMapCopy(),
                    snapshot.apiSuccessMapCopy(),
                    snapshot.apiFailMapCopy(),
                    snapshot.resultsCopy()
            ));
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

        StatsSnapshot snapshot = copySnapshot();
        performanceReportPanel.updateReport(
                snapshot.apiCostMapCopy(),
                snapshot.apiSuccessMapCopy(),
                snapshot.apiFailMapCopy(),
                snapshot.resultsCopy()
        );
    }

    void sampleTrendData() {
        int users = activeThreadsSupplier.getAsInt();
        int activeWebSockets = activeWebSocketsSupplier.getAsInt();
        int activeSseStreams = activeSseStreamsSupplier.getAsInt();
        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        long samplingIntervalMs = samplingIntervalSupplier.getAsLong();
        long windowStart = now - samplingIntervalMs;
        PerformanceRealtimeMetrics.Sample realtimeMetrics = sampleRealtimeMetrics(now);

        CompletableFuture.runAsync(() -> {
            PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                    copyResults(),
                    windowStart,
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
        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                copyResults(),
                now - samplingIntervalMs,
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

    private StatsSnapshot copySnapshot() {
        synchronized (statsLock) {
            List<RequestResult> resultsCopy = new ArrayList<>(allRequestResults);
            Map<String, List<Long>> apiCostMapCopy = new HashMap<>();
            for (Map.Entry<String, List<Long>> entry : apiCostMap.entrySet()) {
                apiCostMapCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return new StatsSnapshot(
                    resultsCopy,
                    apiCostMapCopy,
                    new HashMap<>(apiSuccessMap),
                    new HashMap<>(apiFailMap)
            );
        }
    }

    private List<RequestResult> copyResults() {
        synchronized (statsLock) {
            return new ArrayList<>(allRequestResults);
        }
    }

    private PerformanceRealtimeMetrics.Sample sampleRealtimeMetrics(long nowMs) {
        return realtimeMetricsSampler == null
                ? PerformanceRealtimeMetrics.Sample.empty()
                : realtimeMetricsSampler.apply(nowMs);
    }

    private static RegularTimePeriod createTrendPeriod(long timestampMs) {
        return new Millisecond(new Date(timestampMs));
    }

    private record StatsSnapshot(
            List<RequestResult> resultsCopy,
            Map<String, List<Long>> apiCostMapCopy,
            Map<String, Integer> apiSuccessMapCopy,
            Map<String, Integer> apiFailMapCopy
    ) {
    }
}

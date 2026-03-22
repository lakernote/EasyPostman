package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
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
    private final LongSupplier samplingIntervalSupplier;

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
        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        long samplingIntervalMs = samplingIntervalSupplier.getAsLong();
        long windowStart = now - samplingIntervalMs;

        CompletableFuture.runAsync(() -> {
            TrendSnapshot snapshot = buildTrendSnapshot(windowStart, now);
            SwingUtilities.invokeLater(() -> {
                log.debug("采样数据 {} - 用户数: {}, 平均响应时间: {} ms, QPS: {}, 错误率: {}%, 样本数: {}",
                        period, users, snapshot.avgRespTime(), snapshot.qps(), snapshot.errorPercent(), snapshot.totalReq());
                performanceTrendPanel.addOrUpdate(
                        period,
                        users,
                        snapshot.avgRespTime(),
                        snapshot.qps(),
                        snapshot.errorPercent()
                );
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
        long now = System.currentTimeMillis();
        RegularTimePeriod period = createTrendPeriod(now);
        TrendSnapshot snapshot = buildTrendSnapshot(now - samplingIntervalSupplier.getAsLong(), now);

        log.debug("同步采样数据 {} - 用户数: {}, 平均响应时间: {} ms, QPS: {}, 错误率: {}%, 样本数: {}",
                period, users, snapshot.avgRespTime(), snapshot.qps(), snapshot.errorPercent(), snapshot.totalReq());
        performanceTrendPanel.addOrUpdate(
                period,
                users,
                snapshot.avgRespTime(),
                snapshot.qps(),
                snapshot.errorPercent()
        );
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

    private TrendSnapshot buildTrendSnapshot(long windowStart, long now) {
        int totalReq = 0;
        int errorReq = 0;
        long totalRespTime = 0;
        long actualMinTime = Long.MAX_VALUE;
        long actualMaxTime = 0;

        synchronized (statsLock) {
            for (RequestResult result : allRequestResults) {
                if (result.endTime >= windowStart && result.endTime <= now) {
                    totalReq++;
                    if (!result.success) {
                        errorReq++;
                    }
                    totalRespTime += result.getResponseTime();
                    actualMinTime = Math.min(actualMinTime, result.endTime);
                    actualMaxTime = Math.max(actualMaxTime, result.endTime);
                }
            }
        }

        double avgRespTime = totalReq > 0
                ? BigDecimal.valueOf((double) totalRespTime / totalReq).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0;

        double qps = 0;
        long samplingIntervalMs = samplingIntervalSupplier.getAsLong();
        if (totalReq > 0 && actualMaxTime > actualMinTime) {
            long actualSpanMs = actualMaxTime - actualMinTime;
            qps = totalReq * 1000.0 / actualSpanMs;
        } else if (totalReq > 0) {
            qps = totalReq / (samplingIntervalMs / 1000.0);
        }

        double errorPercent = totalReq > 0 ? (double) errorReq / totalReq * 100 : 0;
        return new TrendSnapshot(totalReq, avgRespTime, qps, errorPercent);
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

    private record TrendSnapshot(int totalReq, double avgRespTime, double qps, double errorPercent) {
    }
}

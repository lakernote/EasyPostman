package com.laker.postman.panel.performance;

import com.laker.postman.service.setting.SettingManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * 性能测试定时器管理器
 *
 * @author laker
 */
@Slf4j
public class PerformanceTimerManager {

    /**
     * 定时任务调度器（后台线程池）
     */
    private ScheduledExecutorService scheduler;

    /**
     * 趋势图采样定时任务
     */
    private ScheduledFuture<?> trendSamplingTask;

    /**
     * 报表刷新定时任务
     */
    private ScheduledFuture<?> reportRefreshTask;

    @Getter
    private long samplingIntervalMs = 1000;

    /**
     * 报表刷新间隔（毫秒）
     */
    private static final int REPORT_REFRESH_INTERVAL_MS = 1000;

    /**
     * 运行状态检查器 - 用于判断是否应该继续执行定时任务
     */
    private final BooleanSupplier runningChecker;

    @Setter
    private Runnable trendSamplingCallback;

    @Setter
    private Runnable reportRefreshCallback;

    /**
     * 管理器是否已启动
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 构造函数
     *
     * @param runningChecker 运行状态检查器，返回 true 表示正在运行
     */
    public PerformanceTimerManager(BooleanSupplier runningChecker) {
        this.runningChecker = runningChecker;
    }

    /**
     * 启动所有定时器
     * 包括趋势图采样定时器和报表刷新定时器
     */
    public void startAll() {
        if (started.getAndSet(true)) {
            log.warn("定时器管理器已经启动，跳过重复启动");
            return;
        }

        // 从设置中读取采样间隔
        int samplingIntervalSeconds = SettingManager.getTrendSamplingIntervalSeconds();
        samplingIntervalMs = samplingIntervalSeconds * 1000L;

        log.info("启动性能测试定时器 - 采样间隔: {}秒, 报表刷新间隔: {}ms",
                samplingIntervalSeconds, REPORT_REFRESH_INTERVAL_MS);

        // 创建定时任务调度器（2个核心线程，足够处理采样和报表刷新）
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "PerformanceTimer-" + System.currentTimeMillis());
            t.setDaemon(true);  // 守护线程，不阻止 JVM 退出
            return t;
        });

        startTrendSamplingTimer();
        startReportRefreshTimer();
    }

    /**
     * 启动趋势图采样定时器
     */
    private void startTrendSamplingTimer() {
        if (trendSamplingCallback == null) {
            log.warn("趋势图采样回调未设置，跳过启动采样定时器");
            return;
        }

        trendSamplingTask = scheduler.scheduleAtFixedRate(() -> {
            if (!runningChecker.getAsBoolean()) {
                log.debug("运行状态检查失败，跳过趋势图采样");
                return;
            }
            try {
                trendSamplingCallback.run();
            } catch (Exception ex) {
                log.error("趋势图采样执行失败", ex);
            }
        }, 0, samplingIntervalMs, TimeUnit.MILLISECONDS);

        log.debug("趋势图采样定时器已启动 - 间隔: {}ms", samplingIntervalMs);
    }

    /**
     * 启动报表刷新定时器
     */
    private void startReportRefreshTimer() {
        if (reportRefreshCallback == null) {
            log.warn("报表刷新回调未设置，跳过启动刷新定时器");
            return;
        }

        reportRefreshTask = scheduler.scheduleAtFixedRate(() -> {
            if (!runningChecker.getAsBoolean()) {
                log.debug("运行状态检查失败，跳过报表刷新");
                return;
            }
            try {
                reportRefreshCallback.run();
            } catch (Exception ex) {
                log.error("报表刷新执行失败", ex);
            }
        }, 0, REPORT_REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.debug("报表刷新定时器已启动 - 间隔: {}ms", REPORT_REFRESH_INTERVAL_MS);
    }

    /**
     * 停止所有定时器
     */
    public void stopAll() {
        if (!started.getAndSet(false)) {
            log.debug("定时器管理器未启动，无需停止");
            return;
        }

        log.info("停止所有性能测试定时器");

        // 取消定时任务
        if (trendSamplingTask != null) {
            trendSamplingTask.cancel(false);
            trendSamplingTask = null;
        }
        if (reportRefreshTask != null) {
            reportRefreshTask.cancel(false);
            reportRefreshTask = null;
        }

        // 关闭调度器
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("定时器未能在 2 秒内正常关闭，强制停止");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("等待定时器关闭时被中断，强制停止");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }

        log.debug("性能测试定时器已停止");
    }

    /**
     * 清理资源
     */
    public void dispose() {
        log.info("销毁定时器管理器");
        stopAll();
        trendSamplingCallback = null;
        reportRefreshCallback = null;
    }
}


package com.laker.postman.panel.performance.control;

import com.laker.postman.panel.performance.runtime.PerformanceThreadFactory;
import com.laker.postman.service.setting.SettingManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
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
     * 是否启用趋势图采样。关闭时不创建采样定时任务，减少采样与图表刷新开销。
     */
    private volatile boolean trendSamplingEnabled = true;

    /**
     * 是否启用运行中报表刷新。默认结束后统一生成，减少压测过程中的快照与表格刷新开销。
     */
    private volatile boolean reportRefreshEnabled = false;

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

        log.info("启动性能测试定时器 - 采样间隔: {}秒, 报表实时刷新: {}",
                samplingIntervalSeconds, reportRefreshEnabled);

        scheduler = Executors.newSingleThreadScheduledExecutor(
                PerformanceThreadFactory.daemonFactory("PerformanceTimer")
        );

        if (trendSamplingEnabled) {
            startTrendSamplingTimer();
        } else {
            log.info("趋势图采样已关闭，跳过采样定时器");
        }
        if (reportRefreshEnabled) {
            startReportRefreshTimer();
        } else {
            log.info("报表实时刷新已关闭，结束后统一生成报表");
        }
    }

    public synchronized void setTrendSamplingEnabled(boolean enabled) {
        if (trendSamplingEnabled == enabled) {
            return;
        }
        trendSamplingEnabled = enabled;
        if (!enabled) {
            stopTrendSamplingTimer();
            log.info("趋势图采样已关闭");
            return;
        }
        if (started.get() && scheduler != null && !scheduler.isShutdown()) {
            startTrendSamplingTimer();
        }
        log.info("趋势图采样已开启");
    }

    public synchronized void setReportRefreshEnabled(boolean enabled) {
        if (reportRefreshEnabled == enabled) {
            return;
        }
        reportRefreshEnabled = enabled;
        if (!enabled) {
            stopReportRefreshTimer();
            log.info("报表实时刷新已关闭");
            return;
        }
        if (started.get() && scheduler != null && !scheduler.isShutdown()) {
            startReportRefreshTimer();
        }
        log.info("报表实时刷新已开启");
    }

    /**
     * 启动趋势图采样定时器
     */
    private void startTrendSamplingTimer() {
        if (!trendSamplingEnabled) {
            return;
        }
        if (trendSamplingCallback == null) {
            log.warn("趋势图采样回调未设置，跳过启动采样定时器");
            return;
        }
        if (scheduler == null || scheduler.isShutdown()) {
            log.warn("定时器调度器未就绪，跳过启动采样定时器");
            return;
        }
        if (trendSamplingTask != null && !trendSamplingTask.isCancelled() && !trendSamplingTask.isDone()) {
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
        }, samplingIntervalMs, samplingIntervalMs, TimeUnit.MILLISECONDS);

        log.debug("趋势图采样定时器已启动 - 间隔: {}ms", samplingIntervalMs);
    }

    private void stopTrendSamplingTimer() {
        if (trendSamplingTask != null) {
            trendSamplingTask.cancel(false);
            trendSamplingTask = null;
        }
    }

    /**
     * 启动报表刷新定时器
     */
    private void startReportRefreshTimer() {
        if (!reportRefreshEnabled) {
            return;
        }
        if (reportRefreshCallback == null) {
            log.warn("报表刷新回调未设置，跳过启动刷新定时器");
            return;
        }
        if (scheduler == null || scheduler.isShutdown()) {
            log.warn("定时器调度器未就绪，跳过启动报表刷新定时器");
            return;
        }
        if (reportRefreshTask != null && !reportRefreshTask.isCancelled() && !reportRefreshTask.isDone()) {
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

    private void stopReportRefreshTimer() {
        if (reportRefreshTask != null) {
            reportRefreshTask.cancel(false);
            reportRefreshTask = null;
        }
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
        stopTrendSamplingTimer();
        stopReportRefreshTimer();

        // 关闭调度器
        ScheduledExecutorService schedulerToStop = scheduler;
        scheduler = null;
        if (schedulerToStop != null) {
            schedulerToStop.shutdown();
            if (SwingUtilities.isEventDispatchThread()) {
                log.debug("在 EDT 上跳过等待定时器关闭，避免阻塞界面");
                return;
            }
            try {
                if (!schedulerToStop.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("定时器未能在 2 秒内正常关闭，强制停止");
                    schedulerToStop.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("等待定时器关闭时被中断，强制停止");
                schedulerToStop.shutdownNow();
                Thread.currentThread().interrupt();
            }
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

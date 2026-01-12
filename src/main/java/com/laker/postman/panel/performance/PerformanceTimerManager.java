package com.laker.postman.panel.performance;

import com.laker.postman.service.setting.SettingManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * 性能测试定时器管理器
 * 统一管理趋势图采样定时器和报表刷新定时器
 * 使用 javax.swing.Timer 确保 EDT 线程安全
 *
 * @author laker
 */
@Slf4j
public class PerformanceTimerManager {

    /**
     * 趋势图采样定时器
     */
    private Timer trendSamplingTimer;

    /**
     * 报表刷新定时器
     */
    private Timer reportRefreshTimer;

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

        startTrendSamplingTimer();
        startReportRefreshTimer();
    }

    /**
     * 启动趋势图采样定时器
     */
    private void startTrendSamplingTimer() {
        stopTrendSamplingTimer();

        if (trendSamplingCallback == null) {
            log.warn("趋势图采样回调未设置，跳过启动采样定时器");
            return;
        }

        trendSamplingTimer = new Timer((int) samplingIntervalMs, e -> {
            if (!runningChecker.getAsBoolean()) {
                log.debug("运行状态检查失败，跳过趋势图采样");
                return;
            }
            try {
                trendSamplingCallback.run();
            } catch (Exception ex) {
                log.error("趋势图采样执行失败", ex);
            }
        });
        trendSamplingTimer.setRepeats(true);
        trendSamplingTimer.setInitialDelay(0); // 立即执行第一次采样
        trendSamplingTimer.start();

        log.debug("趋势图采样定时器已启动 - 间隔: {}ms", samplingIntervalMs);
    }

    /**
     * 启动报表刷新定时器
     */
    private void startReportRefreshTimer() {
        stopReportRefreshTimer();

        if (reportRefreshCallback == null) {
            log.warn("报表刷新回调未设置，跳过启动刷新定时器");
            return;
        }

        reportRefreshTimer = new Timer(REPORT_REFRESH_INTERVAL_MS, e -> {
            if (!runningChecker.getAsBoolean()) {
                log.debug("运行状态检查失败，跳过报表刷新");
                return;
            }
            try {
                reportRefreshCallback.run();
            } catch (Exception ex) {
                log.error("报表刷新执行失败", ex);
            }
        });
        reportRefreshTimer.setRepeats(true);
        reportRefreshTimer.start();

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
        stopTrendSamplingTimer();
        stopReportRefreshTimer();
    }

    /**
     * 停止趋势图采样定时器
     */
    private void stopTrendSamplingTimer() {
        if (trendSamplingTimer != null) {
            trendSamplingTimer.stop();
            trendSamplingTimer = null;
            log.debug("趋势图采样定时器已停止");
        }
    }

    /**
     * 停止报表刷新定时器
     */
    private void stopReportRefreshTimer() {
        if (reportRefreshTimer != null) {
            reportRefreshTimer.stop();
            reportRefreshTimer = null;
            log.debug("报表刷新定时器已停止");
        }
    }

    /**
     * 检查定时器是否正在运行
     *
     * @return true 表示至少有一个定时器在运行
     */
    public boolean isRunning() {
        return started.get() &&
                ((trendSamplingTimer != null && trendSamplingTimer.isRunning()) ||
                        (reportRefreshTimer != null && reportRefreshTimer.isRunning()));
    }

    /**
     * 重新启动所有定时器（会先停止再启动）
     */
    public void restart() {
        log.info("重启性能测试定时器");
        stopAll();
        started.set(false); // 重置启动标志
        startAll();
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


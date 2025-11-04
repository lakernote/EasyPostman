package com.laker.postman.service.update;

import cn.hutool.system.OsInfo;
import com.laker.postman.ioc.Component;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自动更新管理器 - 统一管理所有更新相关功能
 * 采用责任链模式和观察者模式，提供更优雅的更新解决方案
 */
@Slf4j
@Component
public class AutoUpdateManager {

    private final VersionChecker versionChecker;
    private final UpdateUIManager uiManager;
    private final ScheduledExecutorService scheduler;

    public AutoUpdateManager() {
        this.versionChecker = new VersionChecker();
        this.uiManager = new UpdateUIManager();
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "UpdateChecker");
            t.setDaemon(true); // 设置为守护线程，不阻止JVM退出
            return t;
        });
    }

    /**
     * 启动自动更新检查（应用启动时调用）
     */
    public void startBackgroundCheck() {
        boolean autoCheckEnabled = SettingManager.isAutoUpdateCheckEnabled();
        long startupDelaySeconds = SettingManager.getAutoUpdateStartupDelaySeconds();
        long checkIntervalHours = SettingManager.getAutoUpdateCheckIntervalHours();
        String currentVersion = SystemUtil.getCurrentVersion();
        log.info("Current application version: {}", currentVersion);
        OsInfo osInfo = cn.hutool.system.SystemUtil.getOsInfo();
        log.info("Detected operating system: {} ", osInfo);
        if (!autoCheckEnabled) {
            log.info("Auto-update check is disabled");
            return;
        }

        log.info("Starting background update check with {}s delay, interval: {}h",
                startupDelaySeconds, checkIntervalHours);

        // 启动延迟检查
        scheduler.schedule(this::performUpdateCheck, startupDelaySeconds, TimeUnit.SECONDS);

        // 设置定期检查
        scheduler.scheduleAtFixedRate(this::performUpdateCheck,
                checkIntervalHours, checkIntervalHours, TimeUnit.HOURS);
    }

    /**
     * 手动检查更新（从菜单触发）
     */
    public CompletableFuture<UpdateInfo> checkForUpdateManually() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Manual update check initiated");
            return versionChecker.checkForUpdate();
        });
    }

    /**
     * 执行更新检查的核心逻辑
     */
    private void performUpdateCheck() {
        try {
            log.debug("Performing background update check");
            UpdateInfo updateInfo = versionChecker.checkForUpdate();

            handleUpdateCheckResult(updateInfo, false);

        } catch (Exception e) {
            log.warn("Background update check failed: {}", e.getMessage());
        }
    }

    /**
     * 处理更新检查结果
     *
     * @param updateInfo 更新信息
     * @param isManual   是否是手动检查
     */
    public void handleUpdateCheckResult(UpdateInfo updateInfo, boolean isManual) {
        SwingUtilities.invokeLater(() -> {
            switch (updateInfo.getStatus()) {
                case UPDATE_AVAILABLE -> {
                    log.info("Update available: {} -> {}",
                            updateInfo.getCurrentVersion(), updateInfo.getLatestVersion());

                    if (isManual) {
                        // 手动检查直接显示对话框
                        uiManager.showUpdateDialog(updateInfo);
                    } else {
                        // 后台检查显示通知
                        uiManager.showUpdateNotification(updateInfo);
                    }
                }
                case NO_UPDATE -> {
                    if (isManual) {
                        // 只有手动检查时才显示"已是最新版本"
                        showNoUpdateMessage(updateInfo);
                    }
                    log.debug("No update available: {}", updateInfo.getMessage());
                }
                case CHECK_FAILED -> {
                    if (isManual) {
                        // 只有手动检查时才显示错误信息
                        showCheckFailedMessage(updateInfo);
                    }
                    log.debug("Update check failed: {}", updateInfo.getMessage());
                }
            }
        });
    }

    /**
     * 显示无更新消息
     */
    private void showNoUpdateMessage(UpdateInfo updateInfo) {
        NotificationUtil.showInfo(updateInfo.getMessage());
    }

    /**
     * 显示检查失败消息
     */
    private void showCheckFailedMessage(UpdateInfo updateInfo) {
        NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.ERROR_UPDATE_FAILED, updateInfo.getMessage()));
    }

    /**
     * 停止自动更新检查
     */
    public void stopBackgroundCheck() {
        log.info("Stopping background update check");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
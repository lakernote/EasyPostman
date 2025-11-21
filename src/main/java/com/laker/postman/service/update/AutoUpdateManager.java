package com.laker.postman.service.update;

import com.laker.postman.ioc.Component;
import com.laker.postman.model.UpdateCheckFrequency;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 自动更新管理器 - 统一管理所有更新相关功能
 * 每次程序启动时检查，根据配置的频率（每日/每周/每月）和上次检查时间决定是否执行检查
 */
@Slf4j
@Component
public class AutoUpdateManager {

    private final VersionChecker versionChecker;
    private final UpdateUIManager uiManager;

    public AutoUpdateManager() {
        this.versionChecker = new VersionChecker();
        this.uiManager = new UpdateUIManager();
    }

    /**
     * 启动自动更新检查（应用启动时调用）
     * 根据上次检查时间和配置的频率决定是否需要检查更新
     */
    public void startBackgroundCheck() {
        boolean autoCheckEnabled = SettingManager.isAutoUpdateCheckEnabled();
        String currentVersion = SystemUtil.getCurrentVersion();
        log.info("Current application version: {}", currentVersion);
        String osInfo = SystemUtil.getOsInfo();
        log.info("Detected operating system: \n{} ", osInfo);

        if (!autoCheckEnabled) {
            log.info("Auto-update check is disabled");
            return;
        }

        String frequencyCode = SettingManager.getAutoUpdateCheckFrequency();
        UpdateCheckFrequency frequency = UpdateCheckFrequency.fromCode(frequencyCode);
        long lastCheckTime = SettingManager.getLastUpdateCheckTime();
        long currentTime = System.currentTimeMillis();

        log.info("Update check frequency: {}, last check time: {}",
                frequency.getCode(), lastCheckTime > 0 ? new Date(lastCheckTime) : "never");

        // 判断是否需要检查更新
        if (shouldCheckForUpdate(lastCheckTime, currentTime, frequency)) {
            log.info("Performing startup update check...");
            // 延迟2秒后执行检查，避免影响启动速度
            new Thread(() -> {
                try {
                    TimeUnit.SECONDS.sleep(2);
                    performUpdateCheck(false); // false表示自动检查（后台静默检查）
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Update check interrupted: {}", e.getMessage());
                }
            }, "UpdateChecker").start();
        } else {
            log.info("Skipping update check - not yet time according to frequency settings");
        }
    }

    /**
     * 判断是否应该检查更新
     */
    private boolean shouldCheckForUpdate(long lastCheckTime, long currentTime, UpdateCheckFrequency frequency) {
        // 如果设置为每次启动都检查，直接返回true
        if (frequency.isAlwaysCheck()) {
            return true;
        }

        // 如果从未检查过，应该检查
        if (lastCheckTime == 0) {
            return true;
        }

        // 计算距离上次检查的天数
        long daysSinceLastCheck = TimeUnit.MILLISECONDS.toDays(currentTime - lastCheckTime);

        // 根据频率判断
        return daysSinceLastCheck >= frequency.getDays();
    }

    /**
     * 手动检查更新（从菜单触发）
     * 注意：手动检查也会更新"上次检查时间"
     */
    public CompletableFuture<UpdateInfo> checkForUpdateManually() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Manual update check initiated");
            UpdateInfo updateInfo = versionChecker.checkForUpdate();
            // 手动检查也记录时间
            SettingManager.setLastUpdateCheckTime(System.currentTimeMillis());
            log.info("Manual update check completed, timestamp recorded");
            return updateInfo;
        });
    }

    /**
     * 执行更新检查的核心逻辑
     *
     * @param isManual 是否是手动检查（用于决定UI展示方式）
     */
    private void performUpdateCheck(boolean isManual) {
        try {
            log.debug("Performing {} update check", isManual ? "manual" : "background");
            UpdateInfo updateInfo = versionChecker.checkForUpdate();

            // 记录检查时间
            SettingManager.setLastUpdateCheckTime(System.currentTimeMillis());
            log.info("Update check completed, timestamp recorded");

            handleUpdateCheckResult(updateInfo, isManual);

        } catch (Exception e) {
            log.warn("Update check failed: {}", e.getMessage());
            // 即使失败也记录检查时间，避免频繁重试
            SettingManager.setLastUpdateCheckTime(System.currentTimeMillis());
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

}
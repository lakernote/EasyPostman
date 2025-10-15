package com.laker.postman.service;

import com.laker.postman.service.update.AutoUpdateManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 版本更新服务
 */
@Slf4j
public class UpdateService {

    private static UpdateService instance;
    private final AutoUpdateManager autoUpdateManager;

    private UpdateService() {
        this.autoUpdateManager = AutoUpdateManager.getInstance();
    }

    public static UpdateService getInstance() {
        if (instance == null) {
            synchronized (UpdateService.class) {
                if (instance == null) {
                    instance = new UpdateService();
                }
            }
        }
        return instance;
    }

    /**
     * 启动时异步检查更新
     */
    public void checkUpdateOnStartup() {
        // 启动后台更新检查
        autoUpdateManager.startBackgroundCheck();
    }

    /**
     * 手动检查更新（用于菜单调用）
     */
    public void checkUpdateManually() {
        autoUpdateManager.checkForUpdateManually()
                .thenAccept(updateInfo ->
                        autoUpdateManager.handleUpdateCheckResult(updateInfo, true))
                .exceptionally(throwable -> {
                    log.error("Manual update check failed", throwable);
                    return null;
                });
    }

    /**
     * 停止自动更新服务
     */
    public void shutdown() {
        autoUpdateManager.stopBackgroundCheck();
    }
}
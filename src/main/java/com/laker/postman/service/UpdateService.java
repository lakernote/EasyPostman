package com.laker.postman.service;

import com.laker.postman.ioc.Autowired;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PreDestroy;
import com.laker.postman.service.update.AutoUpdateManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 版本更新服务
 */
@Slf4j
@Component
public class UpdateService {

    @Autowired
    private AutoUpdateManager autoUpdateManager;

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
    @PreDestroy
    public void shutdown() {
        autoUpdateManager.stopBackgroundCheck();
    }
}
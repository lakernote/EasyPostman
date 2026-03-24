package com.laker.postman.service;

import com.laker.postman.ioc.Autowired;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PreDestroy;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.service.update.AutoUpdateManager;
import com.laker.postman.service.update.plugin.PluginUpdateCheckResult;
import com.laker.postman.service.update.plugin.PluginUpdateManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 版本更新服务
 */
@Slf4j
@Component
public class UpdateService {

    @Autowired
    private AutoUpdateManager autoUpdateManager;

    @Autowired
    private PluginUpdateManager pluginUpdateManager;

    /**
     * 启动时异步检查更新
     */
    public void checkUpdateOnStartup() {
        // 启动后台更新检查
        autoUpdateManager.startBackgroundCheck();
        pluginUpdateManager.startBackgroundCheck();
    }

    /**
     * 手动检查更新（用于菜单调用）
     */
    public void checkUpdateManually() {
        CompletableFuture<PluginUpdateCheckResult> pluginUpdateFuture = pluginUpdateManager.checkForUpdateManually();
        autoUpdateManager.checkForUpdateManually()
                .thenCombine(pluginUpdateFuture, (updateInfo, pluginUpdateResult) -> {
                    handleManualCheckResult(updateInfo, pluginUpdateResult);
                    return null;
                })
                .exceptionally(throwable -> {
                    log.error("Manual update check failed", throwable);
                    return null;
                });
    }

    private void handleManualCheckResult(UpdateInfo updateInfo, PluginUpdateCheckResult pluginUpdateResult) {
        boolean appNoUpdate = updateInfo != null && updateInfo.getStatus() == UpdateInfo.Status.NO_UPDATE;
        boolean pluginCheckSucceeded = pluginUpdateResult != null && !pluginUpdateResult.failed();
        boolean pluginHasUpdates = pluginUpdateResult != null && pluginUpdateResult.hasUpdates();

        if (appNoUpdate && pluginCheckSucceeded && !pluginHasUpdates) {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.UPDATE_ALREADY_LATEST_WITH_PLUGINS));
            return;
        }

        if (!(appNoUpdate && pluginHasUpdates)) {
            autoUpdateManager.handleUpdateCheckResult(updateInfo, true);
        }
        pluginUpdateManager.handleManualCheckResult(pluginUpdateResult);
    }

}

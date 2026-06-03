package com.laker.postman.service.update.plugin;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.ioc.Component;
import com.laker.postman.platform.update.UpdateCenter;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;
import com.laker.postman.panel.topmenu.plugin.PluginManagerDialog;
import com.laker.postman.panel.update.PluginUpdateNotification;
import com.laker.postman.service.update.AppUpdateCenter;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 插件自动更新提示管理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginUpdateManager {

    private static final ScheduledExecutorService UPDATE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "PluginUpdateChecker");
        thread.setDaemon(true);
        return thread;
    });

    private final PluginUpdateChecker pluginUpdateChecker;
    private final UpdateCenter updateCenter = AppUpdateCenter.get();

    public CompletableFuture<PluginUpdateCheckResult> checkForUpdateManually() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<PluginUpdateCandidate> candidates = pluginUpdateChecker.checkForUpdates();
                updateCenter.recordCheck(UpdateTarget.PLUGIN, System.currentTimeMillis());
                rememberNotifiedCandidates(candidates);
                return PluginUpdateCheckResult.success(candidates);
            } catch (Exception e) {
                log.warn("Manual plugin update check failed: {}", e.getMessage(), e);
                updateCenter.recordCheck(UpdateTarget.PLUGIN, System.currentTimeMillis());
                return PluginUpdateCheckResult.failed(e.getMessage());
            }
        }, UPDATE_EXECUTOR);
    }

    public void startBackgroundCheck() {
        UpdatePolicy policy = updateCenter.policy(UpdateTarget.PLUGIN);
        if (!policy.enabled()) {
            log.info("Plugin auto-update check is disabled");
            return;
        }

        long lastCheckTime = updateCenter.state(UpdateTarget.PLUGIN).lastCheckTimeMillis();
        long currentTime = System.currentTimeMillis();

        log.info("Plugin update check frequency: {}, last check time: {}",
                policy.frequency().getCode(), lastCheckTime > 0 ? new Date(lastCheckTime) : "never");

        if (!updateCenter.shouldCheck(UpdateTarget.PLUGIN, currentTime)) {
            log.info("Skipping plugin update check - not yet time according to frequency settings");
            return;
        }

        UPDATE_EXECUTOR.submit(() -> {
            try {
                performUpdateCheck();
            } catch (Exception e) {
                log.error("Unexpected error in plugin update check task", e);
            }
        });
    }

    private void performUpdateCheck() {
        try {
            List<PluginUpdateCandidate> candidates = pluginUpdateChecker.checkForUpdates();
            updateCenter.recordCheck(UpdateTarget.PLUGIN, System.currentTimeMillis());
            log.info("Plugin update check completed successfully, candidates={}", candidates.size());
            List<PluginUpdateCandidate> unseenCandidates = filterUnseenCandidates(candidates);
            if (unseenCandidates.isEmpty()) {
                log.debug("No new plugin updates need notification");
                return;
            }
            rememberNotifiedCandidates(unseenCandidates);
            SwingUtilities.invokeLater(() -> showUpdateNotification(unseenCandidates));
        } catch (Exception e) {
            log.warn("Plugin update check failed: {}", e.getMessage(), e);
            updateCenter.recordCheck(UpdateTarget.PLUGIN, System.currentTimeMillis());
        }
    }

    private void showUpdateNotification(List<PluginUpdateCandidate> candidates) {
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        PluginUpdateNotification.show(mainFrame, candidates, updates ->
                PluginManagerDialog.showMarketDialog(mainFrame, updates.get(0).pluginId()));
    }

    public void handleManualCheckResult(PluginUpdateCheckResult result) {
        if (result == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (result.failed()) {
                NotificationUtil.showError(I18nUtil.getMessage(
                        MessageKeys.PLUGIN_UPDATE_CHECK_FAILED,
                        result.errorMessage()
                ));
                return;
            }
            if (!result.hasUpdates()) {
                return;
            }
            MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
            int choice = JOptionPane.showConfirmDialog(
                    mainFrame,
                    buildManualPromptMessage(result.candidates()),
                    I18nUtil.getMessage(MessageKeys.PLUGIN_UPDATE_NOTIFICATION_TITLE),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                PluginManagerDialog.showMarketDialog(mainFrame, result.candidates().get(0).pluginId());
            }
        });
    }

    private List<PluginUpdateCandidate> filterUnseenCandidates(List<PluginUpdateCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .filter(candidate -> updateCenter.shouldNotify(UpdateTarget.PLUGIN, toMarker(candidate), false))
                .toList();
    }

    private void rememberNotifiedCandidates(List<PluginUpdateCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (PluginUpdateCandidate candidate : candidates) {
            updateCenter.rememberNotifiedMarker(UpdateTarget.PLUGIN, toMarker(candidate));
        }
    }

    private String toMarker(PluginUpdateCandidate candidate) {
        return candidate.pluginId() + "@" + candidate.latestVersion();
    }

    private String buildManualPromptMessage(List<PluginUpdateCandidate> candidates) {
        PluginUpdateCandidate first = candidates.get(0);
        if (candidates.size() == 1) {
            return I18nUtil.getMessage(
                    MessageKeys.PLUGIN_UPDATE_MANUAL_PROMPT_SINGLE,
                    first.pluginName(),
                    first.installedVersion(),
                    first.latestVersion()
            );
        }
        return I18nUtil.getMessage(
                MessageKeys.PLUGIN_UPDATE_MANUAL_PROMPT_MULTIPLE,
                candidates.size(),
                first.pluginName(),
                first.installedVersion(),
                first.latestVersion()
        );
    }
}

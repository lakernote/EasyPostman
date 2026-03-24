package com.laker.postman.service.update.plugin;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.ioc.Component;
import com.laker.postman.model.UpdateCheckFrequency;
import com.laker.postman.panel.topmenu.plugin.PluginManagerDialog;
import com.laker.postman.panel.update.PluginUpdateNotification;
import com.laker.postman.plugin.runtime.PluginSettingsStore;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 插件自动更新提示管理器。
 */
@Slf4j
@Component
public class PluginUpdateManager {

    private static final String LAST_PLUGIN_UPDATE_CHECK_TIME_KEY = "plugin.market.lastUpdateCheckTime";
    private static final String LAST_PLUGIN_UPDATE_NOTIFIED_KEY = "plugin.market.notifiedVersions";

    private static final ScheduledExecutorService UPDATE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "PluginUpdateChecker");
        thread.setDaemon(true);
        return thread;
    });

    private final PluginUpdateChecker pluginUpdateChecker;

    public PluginUpdateManager() {
        this.pluginUpdateChecker = new PluginUpdateChecker();
    }

    public CompletableFuture<PluginUpdateCheckResult> checkForUpdateManually() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<PluginUpdateCandidate> candidates = pluginUpdateChecker.checkForUpdates();
                setLastCheckTime(System.currentTimeMillis());
                rememberNotifiedCandidates(candidates);
                return PluginUpdateCheckResult.success(candidates);
            } catch (Exception e) {
                log.warn("Manual plugin update check failed: {}", e.getMessage(), e);
                setLastCheckTime(System.currentTimeMillis());
                return PluginUpdateCheckResult.failed(e.getMessage());
            }
        }, UPDATE_EXECUTOR);
    }

    public void startBackgroundCheck() {
        if (!SettingManager.isAutoUpdateCheckEnabled()) {
            log.info("Plugin auto-update check is disabled");
            return;
        }

        String frequencyCode = SettingManager.getAutoUpdateCheckFrequency();
        UpdateCheckFrequency frequency = UpdateCheckFrequency.fromCode(frequencyCode);
        long lastCheckTime = getLastCheckTime();
        long currentTime = System.currentTimeMillis();

        log.info("Plugin update check frequency: {}, last check time: {}",
                frequency.getCode(), lastCheckTime > 0 ? new Date(lastCheckTime) : "never");

        if (!shouldCheckForUpdate(lastCheckTime, currentTime, frequency)) {
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
            setLastCheckTime(System.currentTimeMillis());
            List<PluginUpdateCandidate> unseenCandidates = filterUnseenCandidates(candidates);
            if (unseenCandidates.isEmpty()) {
                log.debug("No new plugin updates need notification");
                return;
            }
            rememberNotifiedCandidates(unseenCandidates);
            SwingUtilities.invokeLater(() -> showUpdateNotification(unseenCandidates));
        } catch (Exception e) {
            log.warn("Plugin update check failed: {}", e.getMessage(), e);
            setLastCheckTime(System.currentTimeMillis());
        }
    }

    private void showUpdateNotification(List<PluginUpdateCandidate> candidates) {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
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
            MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
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

    private boolean shouldCheckForUpdate(long lastCheckTime, long currentTime, UpdateCheckFrequency frequency) {
        if (frequency.isAlwaysCheck()) {
            return true;
        }
        if (lastCheckTime == 0L) {
            return true;
        }
        long daysSinceLastCheck = TimeUnit.MILLISECONDS.toDays(currentTime - lastCheckTime);
        return daysSinceLastCheck >= frequency.getDays();
    }

    private long getLastCheckTime() {
        String value = PluginSettingsStore.getString(LAST_PLUGIN_UPDATE_CHECK_TIME_KEY);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void setLastCheckTime(long timestamp) {
        PluginSettingsStore.putString(LAST_PLUGIN_UPDATE_CHECK_TIME_KEY, String.valueOf(timestamp));
    }

    private List<PluginUpdateCandidate> filterUnseenCandidates(List<PluginUpdateCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Set<String> notified = PluginSettingsStore.getStringSet(LAST_PLUGIN_UPDATE_NOTIFIED_KEY);
        return candidates.stream()
                .filter(candidate -> !notified.contains(toMarker(candidate)))
                .toList();
    }

    private void rememberNotifiedCandidates(List<PluginUpdateCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        Set<String> notified = new LinkedHashSet<>(PluginSettingsStore.getStringSet(LAST_PLUGIN_UPDATE_NOTIFIED_KEY));
        for (PluginUpdateCandidate candidate : candidates) {
            notified.add(toMarker(candidate));
        }
        PluginSettingsStore.putStringSet(LAST_PLUGIN_UPDATE_NOTIFIED_KEY, notified);
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

package com.laker.postman.service.update;

import com.laker.postman.platform.update.UpdateStateStore;
import com.laker.postman.platform.update.model.UpdateCheckState;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;
import com.laker.postman.plugin.runtime.PluginPlatformSettingsStore;
import com.laker.postman.service.setting.SettingManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * App adapter for update policies and check state.
 */
public final class AppUpdateStateStore implements UpdateStateStore {

    private static final String LAST_PLUGIN_UPDATE_CHECK_TIME_KEY = "plugin.market.lastUpdateCheckTime";
    private static final String LAST_PLUGIN_UPDATE_NOTIFIED_KEY = "plugin.market.notifiedVersions";

    @Override
    public UpdatePolicy policy(UpdateTarget target) {
        return switch (normalizeTarget(target)) {
            case APP -> SettingManager.getAppUpdatePolicy();
            case PLUGIN -> SettingManager.getPluginUpdatePolicy();
        };
    }

    @Override
    public UpdateCheckState state(UpdateTarget target) {
        return switch (normalizeTarget(target)) {
            case APP -> UpdateCheckState.of(UpdateTarget.APP, SettingManager.getLastUpdateCheckTime(), Set.of());
            case PLUGIN -> UpdateCheckState.of(
                    UpdateTarget.PLUGIN,
                    getPluginLastCheckTime(),
                    PluginPlatformSettingsStore.getStringSet(LAST_PLUGIN_UPDATE_NOTIFIED_KEY)
            );
        };
    }

    @Override
    public void recordCheck(UpdateTarget target, long timestampMillis) {
        switch (normalizeTarget(target)) {
            case APP -> SettingManager.setLastUpdateCheckTime(timestampMillis);
            case PLUGIN -> PluginPlatformSettingsStore.putString(
                    LAST_PLUGIN_UPDATE_CHECK_TIME_KEY,
                    String.valueOf(Math.max(0L, timestampMillis))
            );
        }
    }

    @Override
    public void rememberNotifiedMarker(UpdateTarget target, String marker) {
        if (marker == null || marker.isBlank()) {
            return;
        }
        switch (normalizeTarget(target)) {
            case APP -> {
                // App update notifications are controlled by explicit ignored markers and check frequency.
            }
            case PLUGIN -> rememberPluginNotifiedMarker(marker);
        }
    }

    @Override
    public Set<String> ignoredMarkers(UpdateTarget target) {
        return switch (normalizeTarget(target)) {
            case APP -> SettingManager.getAppUpdateIgnoredMarkers();
            case PLUGIN -> Set.of();
        };
    }

    @Override
    public void rememberIgnoredMarker(UpdateTarget target, String marker) {
        if (marker == null || marker.isBlank()) {
            return;
        }
        switch (normalizeTarget(target)) {
            case APP -> SettingManager.rememberAppUpdateIgnoredMarker(marker);
            case PLUGIN -> {
                // Plugin update notifications keep their existing one-time marker behavior.
            }
        }
    }

    public static long getPluginLastCheckTime() {
        String value = PluginPlatformSettingsStore.getString(LAST_PLUGIN_UPDATE_CHECK_TIME_KEY);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static void rememberPluginNotifiedMarker(String marker) {
        Set<String> notified = new LinkedHashSet<>(PluginPlatformSettingsStore.getStringSet(LAST_PLUGIN_UPDATE_NOTIFIED_KEY));
        notified.add(marker.trim());
        PluginPlatformSettingsStore.putStringSet(LAST_PLUGIN_UPDATE_NOTIFIED_KEY, notified);
    }

    private static UpdateTarget normalizeTarget(UpdateTarget target) {
        return target == null ? UpdateTarget.APP : target;
    }
}

package com.laker.postman.plugin.runtime;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 插件运行时状态存储。
 * <p>
 * 这里专门管理“禁用”和“待卸载”两类持久化状态，
 * 避免 PluginRuntime 自己同时承担编排和状态读写职责。
 * </p>
 */
final class PluginStateStore {

    private static final String DISABLED_PLUGIN_IDS_KEY = "plugin.disabledIds";
    private static final String PENDING_UNINSTALL_PLUGIN_IDS_KEY = "plugin.pendingUninstallIds";

    private PluginStateStore() {
    }

    static Set<String> getDisabledPluginIds() {
        return PluginSettingsStore.getStringSet(DISABLED_PLUGIN_IDS_KEY);
    }

    static Set<String> getPendingUninstallPluginIds() {
        return PluginSettingsStore.getStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY);
    }

    static boolean isPluginEnabled(String pluginId) {
        return pluginId != null
                && !getDisabledPluginIds().contains(pluginId)
                && !getPendingUninstallPluginIds().contains(pluginId);
    }

    static boolean isPluginPendingUninstall(String pluginId) {
        return pluginId != null && getPendingUninstallPluginIds().contains(pluginId);
    }

    static void setPluginEnabled(String pluginId, boolean enabled) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        Set<String> disabledIds = new LinkedHashSet<>(getDisabledPluginIds());
        if (enabled) {
            disabledIds.remove(pluginId);
            clearPendingUninstall(pluginId);
        } else {
            disabledIds.add(pluginId);
        }
        PluginSettingsStore.putStringSet(DISABLED_PLUGIN_IDS_KEY, disabledIds);
    }

    static void markPluginPendingUninstall(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        Set<String> pendingIds = new LinkedHashSet<>(getPendingUninstallPluginIds());
        pendingIds.add(pluginId);
        PluginSettingsStore.putStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY, pendingIds);

        Set<String> disabledIds = new LinkedHashSet<>(getDisabledPluginIds());
        disabledIds.add(pluginId);
        PluginSettingsStore.putStringSet(DISABLED_PLUGIN_IDS_KEY, disabledIds);
    }

    static void clearPendingUninstall(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        Set<String> pendingIds = new LinkedHashSet<>(getPendingUninstallPluginIds());
        if (pendingIds.remove(pluginId)) {
            PluginSettingsStore.putStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY, pendingIds);
        }
    }

    static void replacePendingUninstallPluginIds(Set<String> pluginIds) {
        PluginSettingsStore.putStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY, pluginIds);
    }
}

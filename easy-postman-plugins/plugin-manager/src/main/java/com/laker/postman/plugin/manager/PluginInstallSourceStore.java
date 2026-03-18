package com.laker.postman.plugin.manager;

import com.laker.postman.plugin.runtime.PluginSettingsStore;

import java.nio.file.Path;

final class PluginInstallSourceStore {

    private static final String SOURCE_TYPE_PREFIX = "plugin.installSource.type.";
    private static final String SOURCE_LOCATION_PREFIX = "plugin.installSource.location.";

    private PluginInstallSourceStore() {
    }

    static PluginInstallSource get(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return null;
        }
        String type = PluginSettingsStore.getString(SOURCE_TYPE_PREFIX + pluginId);
        String location = PluginSettingsStore.getString(SOURCE_LOCATION_PREFIX + pluginId);
        if ((type == null || type.isBlank()) && (location == null || location.isBlank())) {
            return null;
        }
        return new PluginInstallSource(type, location);
    }

    static void recordLocalInstall(String pluginId, Path sourceJar) {
        record(pluginId, PluginInstallSource.TYPE_LOCAL,
                sourceJar == null ? null : sourceJar.toAbsolutePath().normalize().toString());
    }

    static void recordMarketInstall(String pluginId, String downloadUrl) {
        record(pluginId, PluginInstallSource.TYPE_MARKET, downloadUrl);
    }

    static void clear(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        PluginSettingsStore.putString(SOURCE_TYPE_PREFIX + pluginId, null);
        PluginSettingsStore.putString(SOURCE_LOCATION_PREFIX + pluginId, null);
    }

    private static void record(String pluginId, String type, String location) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        PluginSettingsStore.putString(SOURCE_TYPE_PREFIX + pluginId, type);
        PluginSettingsStore.putString(SOURCE_LOCATION_PREFIX + pluginId, location);
    }
}

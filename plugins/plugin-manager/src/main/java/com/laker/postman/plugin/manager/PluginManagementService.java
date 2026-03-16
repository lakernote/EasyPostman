package com.laker.postman.plugin.manager;

import com.laker.postman.plugin.manager.market.PluginCatalogEntry;
import com.laker.postman.plugin.manager.market.PluginCatalogService;
import com.laker.postman.plugin.manager.market.PluginInstallerService;
import com.laker.postman.plugin.runtime.PluginFileInfo;
import com.laker.postman.plugin.runtime.PluginRuntime;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 插件管理门面，统一封装安装、目录与启停相关操作。
 */
@Slf4j
@UtilityClass
public class PluginManagementService {

    public static String getCatalogUrl() {
        return PluginCatalogService.getCatalogUrl();
    }

    public static void saveCatalogUrl(String catalogUrl) {
        PluginCatalogService.saveCatalogUrl(catalogUrl);
    }

    public static String normalizeCatalogLocation(String catalogUrl) {
        return PluginCatalogService.normalizeCatalogLocation(catalogUrl);
    }

    public static List<PluginCatalogEntry> loadCatalog(String catalogUrl) throws Exception {
        return PluginCatalogService.loadCatalog(catalogUrl);
    }

    public static Path getManagedPluginDir() {
        return PluginRuntime.getManagedPluginDir();
    }

    public static Path getPluginCacheDir() {
        return PluginRuntime.getPluginCacheDir();
    }

    public static List<PluginFileInfo> getInstalledPlugins() {
        return PluginRuntime.getInstalledPlugins();
    }

    public static boolean isManagedPlugin(Path jarPath) {
        if (jarPath == null) {
            return false;
        }
        return jarPath.toAbsolutePath().normalize().startsWith(getManagedPluginDir().toAbsolutePath().normalize());
    }

    public static void setPluginEnabled(String pluginId, boolean enabled) {
        PluginRuntime.setPluginEnabled(pluginId, enabled);
    }

    public static String getCurrentAppVersion() {
        return PluginRuntime.getCurrentAppVersion();
    }

    public static PluginFileInfo installPluginJar(Path sourceJar) throws IOException {
        return PluginInstallerService.installPluginJar(sourceJar);
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry) throws Exception {
        return PluginInstallerService.installCatalogPlugin(entry);
    }

    public static boolean uninstallPlugin(String pluginId) {
        boolean removed = false;
        for (PluginFileInfo info : PluginRuntime.getManagedPluginFiles()) {
            if (!pluginId.equals(info.descriptor().id())) {
                continue;
            }
            try {
                removed |= Files.deleteIfExists(info.jarPath());
            } catch (IOException e) {
                log.warn("Failed to delete installed plugin file: {}", info.jarPath(), e);
            }
        }
        if (removed) {
            PluginRuntime.setPluginEnabled(pluginId, true);
        }
        return removed;
    }
}

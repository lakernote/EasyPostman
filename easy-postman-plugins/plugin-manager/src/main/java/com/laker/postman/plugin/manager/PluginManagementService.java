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
 * <p>
 * 这个类对上给 UI 用，对下协调 runtime / market / installer。
 * UI 层尽量只依赖这里，不直接感知底层插件扫描和文件处理细节。
 * </p>
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

    public static String getOfficialCatalogUrl(String source) {
        return PluginCatalogService.getOfficialCatalogUrl(source);
    }

    public static List<PluginCatalogEntry> loadCatalog(String catalogUrl) throws Exception {
        return PluginCatalogService.loadCatalog(catalogUrl);
    }

    public static Path getManagedPluginDir() {
        return PluginRuntime.getManagedPluginDir();
    }

    public static Path getPluginPackageDir() {
        return PluginRuntime.getPluginPackageDir();
    }

    public static List<PluginFileInfo> getInstalledPlugins() {
        return PluginRuntime.getInstalledPlugins();
    }

    public static boolean isManagedPlugin(Path jarPath) {
        if (jarPath == null) {
            return false;
        }
        // 只有托管安装目录里的插件才允许走“卸载”语义；
        // 开发者手工放在其他目录的 jar，只做发现，不做删除。
        return jarPath.toAbsolutePath().normalize().startsWith(getManagedPluginDir().toAbsolutePath().normalize());
    }

    public static void setPluginEnabled(String pluginId, boolean enabled) {
        PluginRuntime.setPluginEnabled(pluginId, enabled);
    }

    public static boolean isPluginPendingUninstall(String pluginId) {
        return PluginRuntime.isPluginPendingUninstall(pluginId);
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

    public static PluginUninstallResult uninstallPlugin(String pluginId) {
        List<PluginFileInfo> managedPlugins = PluginRuntime.getManagedPluginFiles();
        boolean matched = false;
        boolean hasLoadedManagedPlugin = false;
        boolean removed = false;

        for (PluginFileInfo info : managedPlugins) {
            if (!pluginId.equals(info.descriptor().id())) {
                continue;
            }
            matched = true;
            hasLoadedManagedPlugin |= info.loaded();
        }
        if (!matched) {
            return new PluginUninstallResult(false, false);
        }
        if (hasLoadedManagedPlugin) {
            // 已加载插件优先转成“待卸载”，避免当前进程尤其是 Windows 下删不掉 jar
            PluginRuntime.markPluginPendingUninstall(pluginId);
            return new PluginUninstallResult(false, true);
        }

        for (PluginFileInfo info : managedPlugins) {
            if (!pluginId.equals(info.descriptor().id())) {
                continue;
            }
            try {
                removed |= Files.deleteIfExists(info.jarPath());
            } catch (IOException e) {
                log.warn("Failed to delete installed plugin file: {}", info.jarPath(), e);
            }
        }

        boolean stillInstalled = PluginRuntime.getManagedPluginFiles().stream()
                .anyMatch(info -> pluginId.equals(info.descriptor().id()));
        if (stillInstalled) {
            // 即使当前没有 loaded 标记，只要删除后仍残留文件，也退化成待卸载
            PluginRuntime.markPluginPendingUninstall(pluginId);
            return new PluginUninstallResult(removed, true);
        }
        // 已彻底卸载时，把禁用标记清掉，避免后续重装同 id 插件仍处于禁用状态
        PluginRuntime.setPluginEnabled(pluginId, true);
        return new PluginUninstallResult(removed, false);
    }
}

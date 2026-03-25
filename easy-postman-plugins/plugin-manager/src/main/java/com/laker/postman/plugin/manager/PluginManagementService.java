package com.laker.postman.plugin.manager;

import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.manager.market.PluginCatalogEntry;
import com.laker.postman.plugin.manager.market.PluginCatalogService;
import com.laker.postman.plugin.manager.market.PluginCatalogVersionSelector;
import com.laker.postman.plugin.manager.market.PluginInstallController;
import com.laker.postman.plugin.manager.market.PluginInstallerService;
import com.laker.postman.plugin.manager.market.PluginInstallProgressListener;
import com.laker.postman.plugin.runtime.PluginCompatibility;
import com.laker.postman.plugin.runtime.PluginFileInfo;
import com.laker.postman.plugin.runtime.PluginRuntime;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static String getOfficialCatalogUrl(String source) {
        return PluginCatalogService.getOfficialCatalogUrl(source);
    }

    public static String detectOfficialCatalogSource(String catalogUrl) {
        return PluginCatalogService.detectOfficialCatalogSource(catalogUrl);
    }

    public static List<PluginCatalogEntry> loadCatalog(String catalogUrl) throws Exception {
        return PluginCatalogService.loadCatalog(catalogUrl);
    }

    public static List<PluginCatalogEntry> loadBundledOfficialCatalog(String source) throws Exception {
        return PluginCatalogService.loadBundledOfficialCatalog(source);
    }

    public static List<PluginCatalogEntry> selectCatalogEntriesForCurrentHost(List<PluginCatalogEntry> entries) {
        return selectCatalogEntriesForHost(
                entries,
                PluginRuntime.getCurrentAppVersion(),
                PluginRuntime.getCurrentPluginPlatformVersion()
        );
    }

    static List<PluginCatalogEntry> selectCatalogEntriesForHost(List<PluginCatalogEntry> entries,
                                                                String currentAppVersion,
                                                                String currentPlatformVersion) {
        return PluginCatalogVersionSelector.selectForHost(entries, currentAppVersion, currentPlatformVersion);
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

    public static List<PluginFileInfo> getDisplayInstalledPlugins() {
        return sortDisplayInstalledPlugins(PluginRuntime.getInstalledPlugins());
    }

    static List<PluginFileInfo> sortDisplayInstalledPlugins(List<PluginFileInfo> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return List.of();
        }
        return plugins.stream()
                .filter(plugin -> plugin != null && plugin.descriptor() != null
                        && plugin.descriptor().id() != null && !plugin.descriptor().id().isBlank())
                .sorted(DISPLAY_GROUP_COMPARATOR.thenComparing(DISPLAY_PLUGIN_PRIORITY.reversed()))
                .toList();
    }

    public static Map<String, PluginFileInfo> buildPreferredInstalledPluginMap(List<PluginFileInfo> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return Map.of();
        }
        Map<String, PluginFileInfo> selected = new LinkedHashMap<>();
        for (PluginFileInfo plugin : plugins) {
            if (plugin == null || plugin.descriptor() == null) {
                continue;
            }
            String pluginId = plugin.descriptor().id();
            if (pluginId == null || pluginId.isBlank()) {
                continue;
            }
            PluginFileInfo existing = selected.get(pluginId);
            if (existing == null || DISPLAY_PLUGIN_PRIORITY.compare(plugin, existing) > 0) {
                selected.put(pluginId, plugin);
            }
        }
        return selected;
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

    public static String getCurrentPluginPlatformVersion() {
        return PluginRuntime.getCurrentPluginPlatformVersion();
    }

    public static PluginCompatibility evaluateCompatibility(PluginDescriptor descriptor) {
        return PluginRuntime.evaluateCompatibility(descriptor);
    }

    public static PluginCompatibility evaluateCompatibility(PluginCatalogEntry entry) {
        if (entry == null) {
            return PluginRuntime.evaluateCompatibility("", "", "", "");
        }
        return PluginRuntime.evaluateCompatibility(
                entry.minAppVersion(),
                entry.maxAppVersion(),
                entry.minPlatformVersion(),
                entry.maxPlatformVersion()
        );
    }

    public static PluginFileInfo installPluginJar(Path sourceJar) throws IOException {
        PluginFileInfo installed = PluginInstallerService.installPluginJar(sourceJar);
        PluginInstallSourceStore.recordLocalInstall(installed.descriptor().id(), sourceJar);
        return installed;
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry) throws Exception {
        return installCatalogPlugin(entry, PluginInstallProgressListener.NO_OP);
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry,
                                                      PluginInstallProgressListener progressListener) throws Exception {
        return installCatalogPlugin(entry, progressListener, new PluginInstallController());
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry,
                                                      PluginInstallProgressListener progressListener,
                                                      PluginInstallController installController) throws Exception {
        PluginFileInfo installed = PluginInstallerService.installCatalogPlugin(entry, progressListener, installController);
        PluginInstallSourceStore.recordMarketInstall(installed.descriptor().id(), entry.installUrl());
        return installed;
    }

    public static PluginInstallSource getInstallSource(String pluginId) {
        return PluginInstallSourceStore.get(pluginId);
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
        PluginInstallSourceStore.clear(pluginId);
        return new PluginUninstallResult(removed, false);
    }

    private static final Comparator<PluginFileInfo> DISPLAY_PLUGIN_PRIORITY =
            Comparator.<PluginFileInfo>comparingInt(info -> info.enabled() ? 1 : 0)
                    .thenComparingInt(info -> info.compatible() ? 1 : 0)
                    .thenComparing((left, right) -> comparePluginVersion(left, right))
                    .thenComparingInt(info -> info.loaded() ? 1 : 0)
                    .thenComparingInt(info -> isManagedPlugin(info.jarPath()) ? 1 : 0);

    private static final Comparator<PluginFileInfo> DISPLAY_GROUP_COMPARATOR =
            Comparator.<PluginFileInfo, String>comparing(
                    info -> info.descriptor().name() == null || info.descriptor().name().isBlank()
                            ? info.descriptor().id()
                            : info.descriptor().name(),
                    String.CASE_INSENSITIVE_ORDER
            ).thenComparing(info -> info.descriptor().id(), String.CASE_INSENSITIVE_ORDER);

    private static int comparePluginVersion(PluginFileInfo left, PluginFileInfo right) {
        String leftVersion = left == null || left.descriptor() == null ? null : left.descriptor().version();
        String rightVersion = right == null || right.descriptor() == null ? null : right.descriptor().version();
        if (leftVersion == null || rightVersion == null) {
            return 0;
        }
        String normalizedLeft = trimVersionPrefix(leftVersion);
        String normalizedRight = trimVersionPrefix(rightVersion);
        String[] leftTokens = coreVersion(normalizedLeft).split("\\.");
        String[] rightTokens = coreVersion(normalizedRight).split("\\.");
        int length = Math.max(leftTokens.length, rightTokens.length);
        for (int i = 0; i < length; i++) {
            int leftNumber = i < leftTokens.length ? parseVersionToken(leftTokens[i]) : 0;
            int rightNumber = i < rightTokens.length ? parseVersionToken(rightTokens[i]) : 0;
            if (leftNumber != rightNumber) {
                return Integer.compare(leftNumber, rightNumber);
            }
        }
        return compareVersionQualifier(versionQualifier(normalizedLeft), versionQualifier(normalizedRight));
    }

    private static String trimVersionPrefix(String version) {
        return version.startsWith("v") ? version.substring(1) : version;
    }

    private static String coreVersion(String version) {
        int separatorIndex = qualifierSeparatorIndex(version);
        return separatorIndex >= 0 ? version.substring(0, separatorIndex) : version;
    }

    private static String versionQualifier(String version) {
        int separatorIndex = qualifierSeparatorIndex(version);
        return separatorIndex >= 0 && separatorIndex + 1 < version.length()
                ? version.substring(separatorIndex + 1)
                : "";
    }

    private static int qualifierSeparatorIndex(String version) {
        int dash = version.indexOf('-');
        int plus = version.indexOf('+');
        if (dash < 0) {
            return plus;
        }
        if (plus < 0) {
            return dash;
        }
        return Math.min(dash, plus);
    }

    private static int compareVersionQualifier(String leftQualifier, String rightQualifier) {
        boolean leftBlank = leftQualifier == null || leftQualifier.isBlank();
        boolean rightBlank = rightQualifier == null || rightQualifier.isBlank();
        if (leftBlank && rightBlank) {
            return 0;
        }
        if (leftBlank) {
            return 1;
        }
        if (rightBlank) {
            return -1;
        }
        return leftQualifier.compareToIgnoreCase(rightQualifier);
    }

    private static int parseVersionToken(String token) {
        try {
            return Integer.parseInt(token.replaceAll("\\D", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }
}

package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginDescriptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 简单插件运行时：从本地 plugins 目录加载插件 jar。
 * <p>
 * 这里是插件体系的核心入口，主要职责有 4 个：
 * 1. 扫描插件目录并解析 descriptor
 * 2. 决定哪些插件应该被加载（启用、兼容、版本优先）
 * 3. 创建类加载器并调用插件生命周期
 * 4. 维护禁用/待卸载等持久化状态
 * </p>
 */
@Slf4j
public final class PluginRuntime {

    private static final String RUNTIME_PROPERTIES_RESOURCE = "/META-INF/easy-postman/runtime.properties";
    private static final String PLATFORM_VERSION_KEY = "plugin.platform.version";
    private static final PluginRegistry REGISTRY = new PluginRegistry();
    // 当前进程内已成功加载的插件实例、类加载器与文件信息
    private static final List<EasyPostmanPlugin> LOADED_PLUGINS = new ArrayList<>();
    private static final List<URLClassLoader> PLUGIN_CLASSLOADERS = new ArrayList<>();
    private static final List<PluginFileInfo> LOADED_PLUGIN_FILES = new ArrayList<>();
    @Getter
    private static volatile boolean initialized = false;
    private static volatile String cachedAppVersion;
    private static volatile String cachedPlatformVersion;

    private PluginRuntime() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (PluginRuntime.class) {
            if (initialized) {
                return;
            }
            // 启动时先处理上一次退出后遗留的“待卸载”插件文件，
            // 这样后面的扫描结果才是干净的当前状态。
            cleanupPendingUninstallPlugins();

            // 这里先做“选插件”而不是直接扫到什么就加载什么。
            // 运行时会统一处理禁用、待卸载、兼容性和同 id 多版本择优。
            List<PluginFileInfo> loadCandidates = resolveLoadCandidates();

            for (PluginFileInfo pluginFile : loadCandidates) {
                PluginLoader.loadPluginJar(
                        pluginFile.jarPath(),
                        pluginFile.descriptor(),
                        REGISTRY,
                        LOADED_PLUGINS,
                        PLUGIN_CLASSLOADERS,
                        LOADED_PLUGIN_FILES,
                        PluginRuntime.class.getClassLoader()
                );
            }
            // onLoad 负责注册扩展点，onStart 则表示“所有插件都注册完了，可以开始运行”。
            PluginLoader.startPlugins(LOADED_PLUGINS);
            initialized = true;
        }
    }

    public static PluginRegistry getRegistry() {
        return REGISTRY;
    }

    public static List<PluginFileInfo> getInstalledPlugins() {
        List<PluginFileInfo> installed = new ArrayList<>();
        Set<String> disabledPluginIds = PluginStateStore.getDisabledPluginIds();
        Set<String> pendingUninstallPluginIds = PluginStateStore.getPendingUninstallPluginIds();
        for (Path pluginDir : PluginScanner.resolvePluginDirs(getManagedPluginDir())) {
            installed.addAll(PluginScanner.listPluginsFromDirectory(
                    pluginDir,
                    disabledPluginIds,
                    pendingUninstallPluginIds,
                    PluginRuntime::isLoaded
            ));
        }
        return installed;
    }

    public static String getCurrentAppVersion() {
        String version = cachedAppVersion;
        if (version != null) {
            return version;
        }
        synchronized (PluginRuntime.class) {
            if (cachedAppVersion != null) {
                return cachedAppVersion;
            }
            cachedAppVersion = RuntimeVersionResolver.resolveCurrentAppVersion(PluginRuntime.class);
            return cachedAppVersion;
        }
    }

    public static String getCurrentPluginPlatformVersion() {
        String version = cachedPlatformVersion;
        if (version != null) {
            return version;
        }
        synchronized (PluginRuntime.class) {
            if (cachedPlatformVersion != null) {
                return cachedPlatformVersion;
            }
            cachedPlatformVersion = RuntimeVersionResolver.resolveCurrentPlatformVersion(
                    PluginRuntime.class,
                    RUNTIME_PROPERTIES_RESOURCE,
                    PLATFORM_VERSION_KEY
            );
            return cachedPlatformVersion;
        }
    }

    public static Path getManagedPluginDir() {
        return PluginRuntimePaths.managedPluginDir();
    }

    public static Path getPluginPackageDir() {
        return PluginRuntimePaths.pluginPackageDir();
    }

    public static PluginDescriptor inspectPluginJar(Path jarPath) throws IOException {
        return PluginScanner.readDescriptor(jarPath);
    }

    public static PluginCompatibility evaluateCompatibility(PluginDescriptor descriptor) {
        if (descriptor == null) {
            return new PluginCompatibility(false, false, false,
                    getCurrentAppVersion(), getCurrentPluginPlatformVersion(),
                    "", "", "", "");
        }
        return evaluateCompatibility(
                descriptor.minAppVersion(),
                descriptor.maxAppVersion(),
                descriptor.minPlatformVersion(),
                descriptor.maxPlatformVersion()
        );
    }

    public static PluginCompatibility evaluateCompatibility(String minAppVersion,
                                                            String maxAppVersion,
                                                            String minPlatformVersion,
                                                            String maxPlatformVersion) {
        return evaluateCompatibility(
                new RuntimeVersionInfo(getCurrentAppVersion(), getCurrentPluginPlatformVersion()),
                minAppVersion,
                maxAppVersion,
                minPlatformVersion,
                maxPlatformVersion
        );
    }

    private static PluginCompatibility evaluateCompatibility(RuntimeVersionInfo runtimeVersionInfo,
                                                             String minAppVersion,
                                                             String maxAppVersion,
                                                             String minPlatformVersion,
                                                             String maxPlatformVersion) {
        String currentAppVersion = runtimeVersionInfo.currentAppVersion();
        String currentPlatformVersion = runtimeVersionInfo.currentPlatformVersion();

        // app 版本决定“这个宿主发行版是否允许安装”，platform 版本决定“这套插件 SPI 是否还能装配”。
        boolean appVersionCompatible = isVersionInRange(currentAppVersion, minAppVersion, maxAppVersion);
        boolean platformVersionCompatible = isVersionInRange(currentPlatformVersion, minPlatformVersion, maxPlatformVersion);
        return new PluginCompatibility(
                appVersionCompatible && platformVersionCompatible,
                appVersionCompatible,
                platformVersionCompatible,
                currentAppVersion,
                currentPlatformVersion,
                minAppVersion == null ? "" : minAppVersion,
                maxAppVersion == null ? "" : maxAppVersion,
                minPlatformVersion == null ? "" : minPlatformVersion,
                maxPlatformVersion == null ? "" : maxPlatformVersion
        );
    }

    public static void shutdown() {
        synchronized (PluginRuntime.class) {
            try {
                PluginLoader.stopPlugins(LOADED_PLUGINS);
                PluginLoader.closeClassLoaders(PLUGIN_CLASSLOADERS);
                // shutdown 阶段清理待卸载插件，避免已加载 jar 在运行期间删除失败
                cleanupPendingUninstallPlugins();
            } finally {
                // 彻底清空进程内状态，避免 IDE 反复启动时出现脏缓存
                LOADED_PLUGINS.clear();
                PLUGIN_CLASSLOADERS.clear();
                LOADED_PLUGIN_FILES.clear();
                REGISTRY.clear();
                initialized = false;
                cachedAppVersion = null;
                cachedPlatformVersion = null;
            }
        }
    }

    public static boolean isPluginEnabled(String pluginId) {
        return PluginStateStore.isPluginEnabled(pluginId);
    }

    public static void setPluginEnabled(String pluginId, boolean enabled) {
        PluginStateStore.setPluginEnabled(pluginId, enabled);
    }

    public static boolean isPluginPendingUninstall(String pluginId) {
        return PluginStateStore.isPluginPendingUninstall(pluginId);
    }

    public static void markPluginPendingUninstall(String pluginId) {
        PluginStateStore.markPluginPendingUninstall(pluginId);
    }

    public static void clearPendingUninstall(String pluginId) {
        PluginStateStore.clearPendingUninstall(pluginId);
    }

    public static List<PluginFileInfo> getManagedPluginFiles() {
        return PluginScanner.listPluginsFromDirectory(
                getManagedPluginDir(),
                PluginStateStore.getDisabledPluginIds(),
                PluginStateStore.getPendingUninstallPluginIds(),
                PluginRuntime::isLoaded
        );
    }

    public static List<PluginFileInfo> getPluginPackageFiles() {
        return PluginScanner.listPluginsFromDirectory(
                getPluginPackageDir(),
                PluginStateStore.getDisabledPluginIds(),
                PluginStateStore.getPendingUninstallPluginIds(),
                PluginRuntime::isLoaded
        );
    }

    private static List<PluginFileInfo> resolveLoadCandidates() {
        return PluginCandidateResolver.resolveLoadCandidates(
                PluginScanner.resolvePluginDirs(getManagedPluginDir()),
                PluginStateStore.getDisabledPluginIds(),
                PluginStateStore.getPendingUninstallPluginIds(),
                PluginRuntime::isLoaded
        );
    }

    private static boolean isLoaded(Path jarPath) {
        return LOADED_PLUGIN_FILES.stream().anyMatch(info -> info.jarPath().equals(jarPath));
    }

    static void cleanupPendingUninstallPlugins() {
        Set<String> pendingIds = new LinkedHashSet<>(PluginStateStore.getPendingUninstallPluginIds());
        if (pendingIds.isEmpty()) {
            return;
        }
        List<PluginFileInfo> managedPluginFiles = getManagedPluginFiles();

        // 只清理托管安装目录里的副本，不碰 packages 里的本地插件包
        for (PluginFileInfo info : managedPluginFiles) {
            if (!pendingIds.contains(info.descriptor().id())) {
                continue;
            }
            try {
                Files.deleteIfExists(info.jarPath());
            } catch (IOException e) {
                log.warn("Failed to delete pending uninstall plugin file: {}", info.jarPath(), e);
            }
        }

        Set<String> remainingPending = new LinkedHashSet<>();
        for (PluginFileInfo info : getManagedPluginFiles()) {
            if (pendingIds.contains(info.descriptor().id())) {
                remainingPending.add(info.descriptor().id());
            }
        }
        PluginStateStore.replacePendingUninstallPluginIds(remainingPending);
        if (!remainingPending.isEmpty()) {
            log.info("Pending uninstall plugin(s) still remain after cleanup: {}", remainingPending);
        }
    }

    public static void resetForTests() {
        synchronized (PluginRuntime.class) {
            LOADED_PLUGINS.clear();
            PLUGIN_CLASSLOADERS.clear();
            LOADED_PLUGIN_FILES.clear();
            REGISTRY.clear();
            initialized = false;
            cachedAppVersion = null;
            cachedPlatformVersion = null;
            PluginRuntimePaths.resetForTests();
        }
    }

    private static boolean isVersionInRange(String currentVersion, String minVersion, String maxVersion) {
        if (minVersion != null && !minVersion.isBlank()
                && PluginVersionComparator.compare(currentVersion, minVersion) < 0) {
            return false;
        }
        return maxVersion == null || maxVersion.isBlank()
                || PluginVersionComparator.compare(currentVersion, maxVersion) <= 0;
    }

    private record RuntimeVersionInfo(String currentAppVersion, String currentPlatformVersion) {
    }
}

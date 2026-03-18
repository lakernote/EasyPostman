package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginDescriptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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

    private static final String PLUGIN_DESCRIPTOR_PREFIX = "META-INF/easy-postman/";
    private static final String PLUGIN_DESCRIPTOR_SUFFIX = ".properties";
    private static final String RUNTIME_PROPERTIES_RESOURCE = "/META-INF/easy-postman/runtime.properties";
    private static final String PLATFORM_VERSION_KEY = "plugin.platform.version";
    // 已禁用插件列表。禁用后默认在下次启动时不再加载。
    private static final String DISABLED_PLUGIN_IDS_KEY = "plugin.disabledIds";
    // 待卸载插件列表。主要用于已加载插件无法立即删除的场景，例如 Windows 文件锁。
    private static final String PENDING_UNINSTALL_PLUGIN_IDS_KEY = "plugin.pendingUninstallIds";
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
            cleanupPendingUninstallPlugins();

            List<PluginFileInfo> loadCandidates = resolveLoadCandidates();

            for (PluginFileInfo pluginFile : loadCandidates) {
                loadPluginJar(pluginFile.jarPath(), pluginFile.descriptor());
            }
            for (EasyPostmanPlugin plugin : LOADED_PLUGINS) {
                try {
                    plugin.onStart();
                } catch (Exception e) {
                    log.error("Failed to start plugin: {}", plugin.getClass().getName(), e);
                }
            }
            initialized = true;
        }
    }

    public static PluginRegistry getRegistry() {
        return REGISTRY;
    }

    public static List<PluginFileInfo> getInstalledPlugins() {
        List<PluginFileInfo> installed = new ArrayList<>();
        for (Path pluginDir : resolvePluginDirs()) {
            installed.addAll(listPluginsFromDirectory(pluginDir));
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
            String implementationVersion = PluginRuntime.class.getPackage().getImplementationVersion();
            if (implementationVersion != null && !implementationVersion.isBlank()) {
                cachedAppVersion = implementationVersion;
            } else {
                String pomVersion = resolvePomVersion(Paths.get("pom.xml"));
                cachedAppVersion = pomVersion == null || pomVersion.isBlank() ? "dev" : pomVersion;
            }
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
            try (InputStream inputStream = PluginRuntime.class.getResourceAsStream(RUNTIME_PROPERTIES_RESOURCE)) {
                if (inputStream != null) {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    String propertyVersion = properties.getProperty(PLATFORM_VERSION_KEY);
                    if (propertyVersion != null && !propertyVersion.isBlank()) {
                        cachedPlatformVersion = propertyVersion.trim();
                        return cachedPlatformVersion;
                    }
                }
            } catch (IOException ignored) {
            }
            // 本地开发或未打包场景下，从根 pom 回退读取平台版本，避免 compatibility 判断退化成 "dev"。
            String pomValue = resolvePomProperty(Paths.get("pom.xml"), PLATFORM_VERSION_KEY);
            cachedPlatformVersion = pomValue == null || pomValue.isBlank() ? "dev" : pomValue;
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
        return readDescriptor(jarPath);
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
                for (EasyPostmanPlugin plugin : LOADED_PLUGINS) {
                    try {
                        plugin.onStop();
                    } catch (Exception e) {
                        log.warn("Failed to stop plugin: {}", plugin.getClass().getName(), e);
                    }
                }
                for (URLClassLoader classLoader : PLUGIN_CLASSLOADERS) {
                    try {
                        classLoader.close();
                    } catch (IOException e) {
                        log.warn("Failed to close plugin classloader", e);
                    }
                }
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
        return pluginId != null
                && !getDisabledPluginIds().contains(pluginId)
                && !getPendingUninstallPluginIds().contains(pluginId);
    }

    public static void setPluginEnabled(String pluginId, boolean enabled) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        Set<String> disabledIds = new LinkedHashSet<>(getDisabledPluginIds());
        if (enabled) {
            disabledIds.remove(pluginId);
            // 重新启用时，一并取消“待卸载”状态
            clearPendingUninstall(pluginId);
        } else {
            disabledIds.add(pluginId);
        }
        PluginSettingsStore.putStringSet(DISABLED_PLUGIN_IDS_KEY, disabledIds);
    }

    public static boolean isPluginPendingUninstall(String pluginId) {
        return pluginId != null && getPendingUninstallPluginIds().contains(pluginId);
    }

    public static void markPluginPendingUninstall(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        Set<String> pendingIds = new LinkedHashSet<>(getPendingUninstallPluginIds());
        pendingIds.add(pluginId);
        PluginSettingsStore.putStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY, pendingIds);

        // 待卸载插件也要视为 disabled，避免下次启动前再次被选中加载
        Set<String> disabledIds = new LinkedHashSet<>(getDisabledPluginIds());
        disabledIds.add(pluginId);
        PluginSettingsStore.putStringSet(DISABLED_PLUGIN_IDS_KEY, disabledIds);
    }

    public static void clearPendingUninstall(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        Set<String> pendingIds = new LinkedHashSet<>(getPendingUninstallPluginIds());
        if (pendingIds.remove(pluginId)) {
            PluginSettingsStore.putStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY, pendingIds);
        }
    }

    public static List<PluginFileInfo> getManagedPluginFiles() {
        return listPluginsFromDirectory(getManagedPluginDir());
    }

    public static List<PluginFileInfo> getPluginPackageFiles() {
        return listPluginsFromDirectory(getPluginPackageDir());
    }

    private static Set<Path> resolvePluginDirs() {
        Set<Path> dirs = new LinkedHashSet<>();
        String override = System.getProperty("easyPostman.plugins.dir");
        if (override != null && !override.isBlank()) {
            dirs.add(Paths.get(override));
        }
        // 兼容开发期工作目录下的 plugins/，以及用户数据目录下的托管安装目录
        dirs.add(Paths.get(System.getProperty("user.dir"), "plugins"));
        dirs.add(getManagedPluginDir());
        return dirs;
    }

    private static List<PluginFileInfo> resolveLoadCandidates() {
        RuntimeVersionInfo runtimeVersionInfo = new RuntimeVersionInfo(
                getCurrentAppVersion(),
                getCurrentPluginPlatformVersion()
        );
        PluginStateSnapshot pluginStateSnapshot = new PluginStateSnapshot(
                getDisabledPluginIds(),
                getPendingUninstallPluginIds()
        );
        Map<String, PluginFileInfo> selected = new LinkedHashMap<>();
        for (Path pluginDir : resolvePluginDirs()) {
            for (PluginFileInfo candidate : listPluginsFromDirectory(pluginDir, pluginStateSnapshot, runtimeVersionInfo)) {
                if (!candidate.enabled()) {
                    if (pluginStateSnapshot.pendingUninstallPluginIds().contains(candidate.descriptor().id())) {
                        log.info("Skip pending uninstall plugin: {} ({})", candidate.descriptor().id(), candidate.jarPath());
                        continue;
                    }
                    log.info("Skip disabled plugin: {} ({})", candidate.descriptor().id(), candidate.jarPath());
                    continue;
                }
                if (!candidate.compatible()) {
                    PluginCompatibility compatibility = evaluateCompatibility(runtimeVersionInfo,
                            candidate.descriptor().minAppVersion(),
                            candidate.descriptor().maxAppVersion(),
                            candidate.descriptor().minPlatformVersion(),
                            candidate.descriptor().maxPlatformVersion());
                    log.warn("Skip incompatible plugin: {} requires app {}..{} and platform {}..{}, current app {}, current platform {}",
                            candidate.descriptor().id(),
                            emptyToWildcard(candidate.descriptor().minAppVersion()),
                            emptyToWildcard(candidate.descriptor().maxAppVersion()),
                            emptyToWildcard(candidate.descriptor().minPlatformVersion()),
                            emptyToWildcard(candidate.descriptor().maxPlatformVersion()),
                            compatibility.currentAppVersion(),
                            compatibility.currentPlatformVersion());
                    continue;
                }
                // 同一插件 id 出现多个版本时，只保留最高版本候选项
                PluginFileInfo existing = selected.get(candidate.descriptor().id());
                if (existing == null) {
                    selected.put(candidate.descriptor().id(), candidate);
                    continue;
                }
                if (PluginVersionComparator.compare(candidate.descriptor().version(), existing.descriptor().version()) > 0) {
                    selected.put(candidate.descriptor().id(), candidate);
                }
            }
        }
        List<PluginFileInfo> result = new ArrayList<>(selected.values());
        result.sort(Comparator.comparing(info -> info.jarPath().getFileName().toString()));
        return result;
    }

    private static List<PluginFileInfo> listPluginsFromDirectory(Path pluginDir) {
        return listPluginsFromDirectory(pluginDir,
                new PluginStateSnapshot(getDisabledPluginIds(), getPendingUninstallPluginIds()),
                new RuntimeVersionInfo(getCurrentAppVersion(), getCurrentPluginPlatformVersion()));
    }

    private static List<PluginFileInfo> listPluginsFromDirectory(Path pluginDir,
                                                                 PluginStateSnapshot pluginStateSnapshot,
                                                                 RuntimeVersionInfo runtimeVersionInfo) {
        if (pluginDir == null || !Files.isDirectory(pluginDir)) {
            return Collections.emptyList();
        }
        List<PluginFileInfo> plugins = new ArrayList<>();
        try (Stream<Path> stream = Files.list(pluginDir)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            PluginDescriptor descriptor = inspectPluginJar(path);
                            if (descriptor != null) {
                                boolean enabled = isPluginEnabled(descriptor.id(), pluginStateSnapshot);
                                boolean compatible = evaluateCompatibility(runtimeVersionInfo,
                                        descriptor.minAppVersion(),
                                        descriptor.maxAppVersion(),
                                        descriptor.minPlatformVersion(),
                                        descriptor.maxPlatformVersion()).compatible();
                                plugins.add(new PluginFileInfo(descriptor, path, isLoaded(path), enabled, compatible));
                            }
                        } catch (IOException e) {
                            log.warn("Failed to inspect plugin jar: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to scan plugin directory: {}", pluginDir, e);
        }
        return plugins;
    }

    private static void loadPluginJar(Path jarPath, PluginDescriptor descriptor) {
        try {
            // 每个插件使用独立 URLClassLoader，避免依赖相互污染
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()},
                    PluginRuntime.class.getClassLoader());
            Class<?> entryClass = Class.forName(descriptor.entryClass(), true, classLoader);
            Object instance = entryClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof EasyPostmanPlugin plugin)) {
                throw new IllegalStateException("Plugin entry class does not implement EasyPostmanPlugin: " + descriptor.entryClass());
            }
            plugin.onLoad(new PluginContextImpl(descriptor));
            LOADED_PLUGINS.add(plugin);
            PLUGIN_CLASSLOADERS.add(classLoader);
            LOADED_PLUGIN_FILES.add(new PluginFileInfo(descriptor, jarPath, true, true, true));
        } catch (Exception e) {
            log.error("Failed to load plugin jar: {}", jarPath, e);
        }
    }

    private static PluginDescriptor readDescriptor(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            return jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(PLUGIN_DESCRIPTOR_PREFIX))
                    .filter(entry -> entry.getName().endsWith(PLUGIN_DESCRIPTOR_SUFFIX))
                    .findFirst()
                    .map(entry -> {
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            Properties properties = new Properties();
                            properties.load(inputStream);
                            return new PluginDescriptor(
                                    properties.getProperty("plugin.id", jarPath.getFileName().toString()),
                                    properties.getProperty("plugin.name", jarPath.getFileName().toString()),
                                    properties.getProperty("plugin.version", "dev"),
                                    properties.getProperty("plugin.entryClass"),
                                    properties.getProperty("plugin.description", ""),
                                    properties.getProperty("plugin.homepage", ""),
                                    properties.getProperty("plugin.minAppVersion", ""),
                                    properties.getProperty("plugin.maxAppVersion", ""),
                                    properties.getProperty("plugin.minPlatformVersion", ""),
                                    properties.getProperty("plugin.maxPlatformVersion", "")
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(descriptor -> descriptor.entryClass() != null && !descriptor.entryClass().isBlank())
                    .orElse(null);
        }
    }

    private static boolean isLoaded(Path jarPath) {
        return LOADED_PLUGIN_FILES.stream().anyMatch(info -> info.jarPath().equals(jarPath));
    }

    private static Set<String> getDisabledPluginIds() {
        return PluginSettingsStore.getStringSet(DISABLED_PLUGIN_IDS_KEY);
    }

    private static Set<String> getPendingUninstallPluginIds() {
        return PluginSettingsStore.getStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY);
    }

    static void cleanupPendingUninstallPlugins() {
        Set<String> pendingIds = new LinkedHashSet<>(getPendingUninstallPluginIds());
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
        PluginSettingsStore.putStringSet(PENDING_UNINSTALL_PLUGIN_IDS_KEY, remainingPending);
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

    private static boolean isPluginEnabled(String pluginId, PluginStateSnapshot pluginStateSnapshot) {
        return pluginId != null
                && !pluginStateSnapshot.disabledPluginIds().contains(pluginId)
                && !pluginStateSnapshot.pendingUninstallPluginIds().contains(pluginId);
    }

    private static boolean isVersionInRange(String currentVersion, String minVersion, String maxVersion) {
        if (minVersion != null && !minVersion.isBlank()
                && PluginVersionComparator.compare(currentVersion, minVersion) < 0) {
            return false;
        }
        return maxVersion == null || maxVersion.isBlank()
                || PluginVersionComparator.compare(currentVersion, maxVersion) <= 0;
    }

    private static String emptyToWildcard(String value) {
        return value == null || value.isBlank() ? "*" : value;
    }

    private static String resolvePomVersion(Path pom) {
        String version = resolvePomProperty(pom, "version");
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.trim();
        if ("${revision}".equals(trimmed)) {
            return resolvePomProperty(pom, "revision");
        }
        return trimmed.contains("${") ? null : trimmed;
    }

    private static String resolvePomProperty(Path pom, String propertyName) {
        Path current = pom;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            try {
                if (Files.exists(current)) {
                    String xml = Files.readString(current);
                    String property = readXmlTag(xml, propertyName);
                    if (property != null && !property.isBlank()) {
                        return property.trim();
                    }
                    String relativePath = readXmlTag(xml, "relativePath");
                    if (relativePath != null && !relativePath.isBlank()) {
                        current = current.getParent().resolve(relativePath.trim()).normalize();
                        continue;
                    }
                }
            } catch (Exception ignored) {
            }
            current = current.getParent() == null ? null : current.getParent().getParent() == null ? null : current.getParent().getParent().resolve("pom.xml");
        }
        return null;
    }

    private static String readXmlTag(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf(openTag);
        if (start < 0) {
            return null;
        }
        int contentStart = start + openTag.length();
        int end = xml.indexOf(closeTag, contentStart);
        if (end <= contentStart) {
            return null;
        }
        return xml.substring(contentStart, end).trim();
    }

    private record RuntimeVersionInfo(String currentAppVersion, String currentPlatformVersion) {
    }

    private record PluginStateSnapshot(Set<String> disabledPluginIds, Set<String> pendingUninstallPluginIds) {
    }

    private static final class PluginContextImpl implements PluginContext {
        private final PluginDescriptor descriptor;

        private PluginContextImpl(PluginDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public void registerScriptApi(String alias, java.util.function.Supplier<Object> factory) {
            REGISTRY.registerScriptApi(alias, factory);
        }

        @Override
        public <T> void registerService(Class<T> type, T service) {
            REGISTRY.registerService(type, service);
        }

        @Override
        public void registerToolboxContribution(com.laker.postman.plugin.api.ToolboxContribution contribution) {
            REGISTRY.registerToolboxContribution(contribution);
        }

        @Override
        public void registerScriptCompletionContributor(com.laker.postman.plugin.api.ScriptCompletionContributor contributor) {
            REGISTRY.registerScriptCompletionContributor(contributor);
        }

        @Override
        public void registerSnippet(com.laker.postman.plugin.api.SnippetDefinition definition) {
            REGISTRY.registerSnippet(definition);
        }
    }
}

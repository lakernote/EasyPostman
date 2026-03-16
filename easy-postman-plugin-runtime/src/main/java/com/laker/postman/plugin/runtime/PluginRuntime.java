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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * 简单插件运行时：从本地 plugins 目录加载插件 jar。
 */
@Slf4j
public final class PluginRuntime {

    private static final String PLUGIN_DESCRIPTOR_PREFIX = "META-INF/easy-postman/";
    private static final String PLUGIN_DESCRIPTOR_SUFFIX = ".properties";
    private static final String DISABLED_PLUGIN_IDS_KEY = "plugin.disabledIds";
    private static final String PENDING_UNINSTALL_PLUGIN_IDS_KEY = "plugin.pendingUninstallIds";
    private static final PluginRegistry REGISTRY = new PluginRegistry();
    private static final List<EasyPostmanPlugin> LOADED_PLUGINS = new ArrayList<>();
    private static final List<URLClassLoader> PLUGIN_CLASSLOADERS = new ArrayList<>();
    private static final List<PluginFileInfo> LOADED_PLUGIN_FILES = new ArrayList<>();
    @Getter
    private static volatile boolean initialized = false;

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
            for (PluginFileInfo pluginFile : resolveLoadCandidates()) {
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
            log.info("Plugin runtime initialized, loaded {} plugin(s)", LOADED_PLUGINS.size());
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
        String version = PluginRuntime.class.getPackage().getImplementationVersion();
        if (version != null && !version.isBlank()) {
            return version;
        }
        String pomVersion = resolvePomVersion(Paths.get("pom.xml"));
        return pomVersion == null || pomVersion.isBlank() ? "dev" : pomVersion;
    }

    public static Path getManagedPluginDir() {
        return PluginRuntimePaths.managedPluginDir();
    }

    public static Path getPluginCacheDir() {
        return PluginRuntimePaths.pluginCacheDir();
    }

    public static PluginDescriptor inspectPluginJar(Path jarPath) throws IOException {
        return readDescriptor(jarPath);
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
                cleanupPendingUninstallPlugins();
            } finally {
                LOADED_PLUGINS.clear();
                PLUGIN_CLASSLOADERS.clear();
                LOADED_PLUGIN_FILES.clear();
                REGISTRY.clear();
                initialized = false;
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

    public static List<PluginFileInfo> getCachedPluginFiles() {
        return listPluginsFromDirectory(getPluginCacheDir());
    }

    private static Set<Path> resolvePluginDirs() {
        Set<Path> dirs = new LinkedHashSet<>();
        String override = System.getProperty("easyPostman.plugins.dir");
        if (override != null && !override.isBlank()) {
            dirs.add(Paths.get(override));
        }
        dirs.add(Paths.get(System.getProperty("user.dir"), "plugins"));
        dirs.add(getManagedPluginDir());
        return dirs;
    }

    private static List<PluginFileInfo> resolveLoadCandidates() {
        Map<String, PluginFileInfo> selected = new LinkedHashMap<>();
        for (Path pluginDir : resolvePluginDirs()) {
            for (PluginFileInfo candidate : listPluginsFromDirectory(pluginDir)) {
                if (!candidate.enabled()) {
                    if (isPluginPendingUninstall(candidate.descriptor().id())) {
                        log.info("Skip pending uninstall plugin: {} ({})", candidate.descriptor().id(), candidate.jarPath());
                        continue;
                    }
                    log.info("Skip disabled plugin: {} ({})", candidate.descriptor().id(), candidate.jarPath());
                    continue;
                }
                if (!candidate.compatible()) {
                    log.warn("Skip incompatible plugin: {} requires app {}..{}, current {}",
                            candidate.descriptor().id(),
                            emptyToWildcard(candidate.descriptor().minAppVersion()),
                            emptyToWildcard(candidate.descriptor().maxAppVersion()),
                            getCurrentAppVersion());
                    continue;
                }
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
                                boolean enabled = isPluginEnabled(descriptor.id());
                                boolean compatible = isDescriptorCompatible(descriptor);
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
                                    properties.getProperty("plugin.maxAppVersion", "")
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

        for (PluginFileInfo info : getManagedPluginFiles()) {
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
            PluginRuntimePaths.resetForTests();
        }
    }

    private static boolean isDescriptorCompatible(PluginDescriptor descriptor) {
        String currentVersion = getCurrentAppVersion();
        if (descriptor.hasMinAppVersion()
                && PluginVersionComparator.compare(currentVersion, descriptor.minAppVersion()) < 0) {
            return false;
        }
        return !descriptor.hasMaxAppVersion()
                || PluginVersionComparator.compare(currentVersion, descriptor.maxAppVersion()) <= 0;
    }

    private static String emptyToWildcard(String value) {
        return value == null || value.isBlank() ? "*" : value;
    }

    private static String resolvePomVersion(Path pom) {
        Path current = pom;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            try {
                if (Files.exists(current)) {
                    String xml = Files.readString(current);
                    String version = readXmlTag(xml, "version");
                    if (version != null && !version.isBlank()) {
                        String trimmed = version.trim();
                        if ("${revision}".equals(trimmed)) {
                            String revision = readXmlTag(xml, "revision");
                            if (revision != null && !revision.isBlank()) {
                                return revision.trim();
                            }
                        } else if (!trimmed.contains("${")) {
                            return trimmed;
                        }
                    }
                    String revision = readXmlTag(xml, "revision");
                    if (revision != null && !revision.isBlank()) {
                        return revision.trim();
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

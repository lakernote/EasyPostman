package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * 插件扫描器。
 * <p>
 * 负责目录遍历、descriptor 读取，以及把磁盘上的 jar 转成可供运行时决策的候选信息。
 * </p>
 */
@Slf4j
final class PluginScanner {

    private static final String PLUGIN_DESCRIPTOR_PREFIX = "META-INF/easy-postman/";
    private static final String PLUGIN_DESCRIPTOR_SUFFIX = ".properties";

    private PluginScanner() {
    }

    static Set<Path> resolvePluginDirs(Path managedPluginDir) {
        Set<Path> dirs = new LinkedHashSet<>();
        String override = System.getProperty("easyPostman.plugins.dir");
        if (override != null && !override.isBlank()) {
            dirs.add(Paths.get(override));
        }
        dirs.add(Paths.get(System.getProperty("user.dir"), "plugins"));
        dirs.add(managedPluginDir);
        return dirs;
    }

    static List<PluginFileInfo> listPluginsFromDirectory(Path pluginDir,
                                                         Set<String> disabledPluginIds,
                                                         Set<String> pendingUninstallPluginIds,
                                                         Predicate<Path> isLoadedPredicate) {
        if (pluginDir == null || !Files.isDirectory(pluginDir)) {
            return Collections.emptyList();
        }
        List<PluginFileInfo> plugins = new ArrayList<>();
        try (Stream<Path> stream = Files.list(pluginDir)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            PluginDescriptor descriptor = readDescriptor(path);
                            if (descriptor != null) {
                                boolean enabled = descriptor.id() != null
                                        && !disabledPluginIds.contains(descriptor.id())
                                        && !pendingUninstallPluginIds.contains(descriptor.id());
                                boolean compatible = PluginRuntime.evaluateCompatibility(
                                        descriptor.minAppVersion(),
                                        descriptor.maxAppVersion(),
                                        descriptor.minPlatformVersion(),
                                        descriptor.maxPlatformVersion()
                                ).compatible();
                                plugins.add(new PluginFileInfo(
                                        descriptor,
                                        path,
                                        isLoadedPredicate != null && isLoadedPredicate.test(path),
                                        enabled,
                                        compatible
                                ));
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

    static PluginDescriptor readDescriptor(Path jarPath) throws IOException {
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
}

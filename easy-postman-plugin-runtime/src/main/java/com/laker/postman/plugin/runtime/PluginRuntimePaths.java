package com.laker.postman.plugin.runtime;

import com.laker.postman.util.AppRuntimeLayout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class PluginRuntimePaths {

    private static String cachedDataPath;

    private PluginRuntimePaths() {
    }

    static Path managedPluginDir() {
        Path pluginDir = isPortableMode()
                ? defaultDiscoveryPluginDir()
                : dataRoot().resolve("plugins").resolve("installed");
        try {
            Files.createDirectories(pluginDir);
        } catch (Exception ignored) {
            // defer to callers if directory creation later fails
        }
        return pluginDir;
    }

    static Path pluginPackageDir() {
        Path pluginDir = isPortableMode()
                ? defaultDiscoveryPluginDir().resolve("packages")
                : dataRoot().resolve("plugins").resolve("packages");
        try {
            Files.createDirectories(pluginDir);
        } catch (Exception ignored) {
            // defer to callers if directory creation later fails
        }
        return pluginDir;
    }

    static Path userSettingsFile() {
        return dataRoot().resolve("user_settings.json");
    }

    static void resetForTests() {
        cachedDataPath = null;
    }

    static Path defaultDiscoveryPluginDir() {
        return AppRuntimeLayout.applicationRootDirectory(PluginRuntimePaths.class).resolve("plugins");
    }

    private static Path dataRoot() {
        if (cachedDataPath != null) {
            return Paths.get(cachedDataPath);
        }

        Path root;
        // 测试和开发调试可显式覆盖数据目录，避免污染用户真实目录。
        String override = System.getProperty("easyPostman.data.dir");
        if (override != null && !override.isBlank()) {
            root = Paths.get(override.trim());
        } else {
            root = AppRuntimeLayout.dataRootDirectory(PluginRuntimePaths.class);
        }
        try {
            Files.createDirectories(root);
        } catch (Exception ignored) {
            // callers will surface actual failures
        }
        cachedDataPath = root.toAbsolutePath().normalize().toString();
        return root;
    }

    private static boolean isPortableMode() {
        return AppRuntimeLayout.isPortableMode(PluginRuntimePaths.class);
    }
}

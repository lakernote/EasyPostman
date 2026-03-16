package com.laker.postman.plugin.runtime;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class PluginRuntimePaths {

    private static String cachedDataPath;

    private PluginRuntimePaths() {
    }

    static Path managedPluginDir() {
        Path pluginDir = dataRoot().resolve("plugins").resolve("installed");
        try {
            Files.createDirectories(pluginDir);
        } catch (Exception ignored) {
            // defer to callers if directory creation later fails
        }
        return pluginDir;
    }

    static Path pluginPackageDir() {
        Path pluginDir = dataRoot().resolve("plugins").resolve("packages");
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

    private static Path dataRoot() {
        if (cachedDataPath != null) {
            return Paths.get(cachedDataPath);
        }

        Path root;
        String override = System.getProperty("easyPostman.data.dir");
        if (override != null && !override.isBlank()) {
            root = Paths.get(override.trim());
        } else if (isPortableMode()) {
            root = applicationDirectory().resolve("data");
        } else {
            root = Paths.get(System.getProperty("user.home"), "EasyPostman");
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
        String flag = System.getProperty("easyPostman.portable");
        if (flag != null) {
            return Boolean.parseBoolean(flag);
        }
        return new File(applicationDirectory().toFile(), ".portable").exists();
    }

    private static Path applicationDirectory() {
        try {
            String path = PluginRuntimePaths.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(decoded);
            return file.isFile() ? file.getParentFile().toPath() : Paths.get(System.getProperty("user.dir"));
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir"));
        }
    }
}

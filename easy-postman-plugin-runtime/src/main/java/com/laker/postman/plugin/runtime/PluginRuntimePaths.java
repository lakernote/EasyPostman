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
        return applicationRootDirectory().resolve("plugins");
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
        } else if (isPortableMode()) {
            root = applicationRootDirectory().resolve("data");
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
        Path applicationRoot = applicationRootDirectory();
        if (new File(applicationRoot.toFile(), ".portable").exists()) {
            return true;
        }
        Path codeSourceDirectory = codeSourceDirectory();
        return !applicationRoot.equals(codeSourceDirectory)
                && new File(codeSourceDirectory.toFile(), ".portable").exists();
    }

    private static Path applicationRootDirectory() {
        String override = System.getProperty("easyPostman.app.dir");
        if (override != null && !override.isBlank()) {
            return Paths.get(override.trim()).toAbsolutePath().normalize();
        }

        Path codeSourceDirectory = codeSourceDirectory();
        Path parent = codeSourceDirectory.getParent();
        if (parent != null
                && "app".equalsIgnoreCase(String.valueOf(codeSourceDirectory.getFileName()))
                && Files.isDirectory(parent.resolve("runtime"))) {
            return parent.toAbsolutePath().normalize();
        }
        return codeSourceDirectory.toAbsolutePath().normalize();
    }

    private static Path codeSourceDirectory() {
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

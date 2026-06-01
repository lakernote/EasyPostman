package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 统一解析应用运行形态和关键路径。
 */
@Slf4j
public final class AppRuntimeLayout {

    private static final String PORTABLE_MARKER = ".portable";

    private AppRuntimeLayout() {
    }

    public static boolean isPortableMode(Class<?> anchorClass) {
        String override = System.getProperty("easyPostman.portable");
        if (override != null && !override.isBlank()) {
            return Boolean.parseBoolean(override);
        }

        Path applicationRoot = applicationRootDirectory(anchorClass);
        if (hasPortableMarker(applicationRoot)) {
            return true;
        }

        Path codeSourceDirectory = codeSourceDirectory(anchorClass);
        return !applicationRoot.equals(codeSourceDirectory) && hasPortableMarker(codeSourceDirectory);
    }

    public static Path applicationRootDirectory(Class<?> anchorClass) {
        String override = System.getProperty("easyPostman.app.dir");
        if (override != null && !override.isBlank()) {
            return Paths.get(override.trim()).toAbsolutePath().normalize();
        }

        Path codeSourceDirectory = codeSourceDirectory(anchorClass);
        Path parent = codeSourceDirectory.getParent();
        if (parent != null
                && "app".equalsIgnoreCase(String.valueOf(codeSourceDirectory.getFileName()))
                && parent.resolve("runtime").toFile().isDirectory()) {
            return parent.toAbsolutePath().normalize();
        }
        return codeSourceDirectory.toAbsolutePath().normalize();
    }

    public static Path codeSourceDirectory(Class<?> anchorClass) {
        try {
            String path = anchorClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(decoded);
            if (file.isFile()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    return parent.toPath().toAbsolutePath().normalize();
                }
            }
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
    }

    public static Path dataRootDirectory(Class<?> anchorClass) {
        String override = System.getProperty("easyPostman.data.dir");
        if (override != null && !override.isBlank()) {
            return Paths.get(override.trim()).toAbsolutePath().normalize();
        }
        if (isPortableMode(anchorClass)) {
            Path applicationRoot = applicationRootDirectory(anchorClass);
            return resolvePortableDataDirectory(applicationRoot, codeSourceDirectory(anchorClass));
        }
        return Paths.get(System.getProperty("user.home"), "EasyPostman").toAbsolutePath().normalize();
    }

    public static Path logRootDirectory(Class<?> anchorClass) {
        if (isPortableMode(anchorClass)) {
            return applicationRootDirectory(anchorClass).resolve("logs").toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.home"), "EasyPostman", "logs").toAbsolutePath().normalize();
    }

    private static boolean hasPortableMarker(Path directory) {
        if (directory == null) {
            return false;
        }
        return directory.resolve(PORTABLE_MARKER).toFile().isFile();
    }

    static Path resolvePortableDataDirectory(Path applicationRoot, Path codeSourceDirectory) {
        Path normalizedAppRoot = applicationRoot.toAbsolutePath().normalize();
        Path normalizedCodeSource = codeSourceDirectory.toAbsolutePath().normalize();
        if (!normalizedCodeSource.equals(normalizedAppRoot)
                && "app".equalsIgnoreCase(String.valueOf(normalizedCodeSource.getFileName()))) {
            return normalizedCodeSource.resolve("data").toAbsolutePath().normalize();
        }
        return normalizedAppRoot.resolve("data").toAbsolutePath().normalize();
    }

}

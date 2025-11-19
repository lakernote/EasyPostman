package com.laker.postman.service.update;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Windows 注册表工具类
 * 用于检查应用是否已安装（通过注册表）
 * 参考 Termora 的实现，但不依赖 JNA 库
 */
@Slf4j
public class WindowsRegistryChecker {

    // 64位注册表路径（默认）
    private static final String UNINSTALL_KEY_PATH_64 =
        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E}_is1";

    // 32位注册表路径（在64位系统上的WOW6432Node）
    private static final String UNINSTALL_KEY_PATH_32 =
        "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E}_is1";

    // 用于其他方法的默认路径
    private static final String UNINSTALL_KEY_PATH = UNINSTALL_KEY_PATH_64;

    // 实际找到的注册表路径（缓存）
    private static String actualKeyPath = null;

    /**
     * 检查应用是否已安装（通过注册表）
     * 会同时检查64位和32位注册表路径
     *
     * @return true 如果应用已安装，false 否则
     */
    public static boolean isAppInstalled() {
        // 先检查64位路径
        log.info("Checking 64-bit registry path...");
        if (checkRegistryPath(UNINSTALL_KEY_PATH_64)) {
            actualKeyPath = UNINSTALL_KEY_PATH_64;
            log.info("✓ App is INSTALLED (found in 64-bit registry)");
            return true;
        }

        // 再检查32位路径
        log.info("Checking 32-bit registry path (WOW6432Node)...");
        if (checkRegistryPath(UNINSTALL_KEY_PATH_32)) {
            actualKeyPath = UNINSTALL_KEY_PATH_32;
            log.info("✓ App is INSTALLED (found in 32-bit registry)");
            return true;
        }

        // 尝试通过搜索 DisplayName 查找应用
        log.info("Registry key not found, trying to search by DisplayName...");
        if (findAppByDisplayName()) {
            log.info("✓ App is INSTALLED (found by DisplayName search)");
            return true;
        }

        log.info("✗ App is NOT INSTALLED (not found in registry)");
        return false;
    }

    /**
     * 检查指定的注册表路径是否存在
     */
    private static boolean checkRegistryPath(String keyPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", keyPath);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出（用于日志调试）- 使用GBK编码避免中文乱码
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("  Registry output: {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.info("  Exit code: {}", exitCode);
            return exitCode == 0;

        } catch (Exception e) {
            log.warn("Failed to check registry path {}: {}", keyPath, e.getMessage());
            return false;
        }
    }

    /**
     * 通过搜索 DisplayName 查找应用
     * 使用 reg query /s /f 命令高效搜索注册表
     *
     * @return true 如果找到应用，false 否则
     */
    private static boolean findAppByDisplayName() {
        String[] searchPaths = {
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        };

        for (String basePath : searchPaths) {
            try {
                // 使用 /s 搜索子键, /f 查找指定字符串
                ProcessBuilder pb = new ProcessBuilder("reg", "query", basePath, "/s", "/f", "EasyPostman");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String foundKeyPath = null;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "GBK"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        log.info("  Search output: {}", line);

                        // 找到包含 HKEY 的行（注册表键路径）
                        if (line.startsWith("HKEY_") && line.contains("Uninstall")) {
                            foundKeyPath = line;
                        }

                        // 确认找到的是 DisplayName
                        if (foundKeyPath != null && line.contains("DisplayName") && line.contains("EasyPostman")) {
                            actualKeyPath = foundKeyPath;
                            log.info("✓ Found app at: {}", foundKeyPath);
                            return true;
                        }
                    }
                }

                process.waitFor();

            } catch (Exception e) {
                log.info("Failed to search registry path {}: {}", basePath, e.getMessage());
            }
        }

        return false;
    }


    /**
     * 获取已安装应用的版本
     *
     * @return 已安装的版本号，如果未安装或查询失败则返回 null
     */
    public static String getInstalledVersion() {
        // 使用实际找到的路径，如果没有则尝试默认路径
        String keyPath = actualKeyPath != null ? actualKeyPath : UNINSTALL_KEY_PATH;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", keyPath, "/v", "DisplayVersion"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 解析版本号: DisplayVersion    REG_SZ    4.0.9
                    if (line.contains("DisplayVersion") && line.contains("REG_SZ")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            String version = parts[parts.length - 1];
                            log.info("Installed version from registry: {}", version);
                            return version;
                        }
                    }
                }
            }

            process.waitFor();

        } catch (Exception e) {
            log.warn("Failed to get installed version: {}", e.getMessage());
        }

        return null;
    }
}
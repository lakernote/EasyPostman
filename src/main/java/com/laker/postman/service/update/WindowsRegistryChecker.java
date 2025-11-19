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

    private static final String UNINSTALL_KEY_PATH =
        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E}_is1";

    /**
     * 检查应用是否已安装（通过注册表）
     *
     * @return true 如果应用已安装，false 否则
     */
    public static boolean isAppInstalled() {
        try {
            // 使用 reg query 命令查询注册表
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", UNINSTALL_KEY_PATH, "/ve"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 如果能查询到注册表键，说明已安装
                    if (line.contains("EasyPostman") || line.contains("(Default)")) {
                        log.debug("App is installed (found in registry)");
                        return true;
                    }
                }
            }

            int exitCode = process.waitFor();
            // 退出码 0 表示成功找到注册表键
            boolean installed = exitCode == 0;
            log.debug("Registry check exit code: {}, installed: {}", exitCode, installed);
            return installed;

        } catch (Exception e) {
            log.warn("Failed to check registry: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取已安装应用的版本
     *
     * @return 已安装的版本号，如果未安装或查询失败则返回 null
     */
    public static String getInstalledVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", UNINSTALL_KEY_PATH, "/v", "DisplayVersion"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 解析版本号: DisplayVersion    REG_SZ    4.0.9
                    if (line.contains("DisplayVersion") && line.contains("REG_SZ")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            String version = parts[parts.length - 1];
                            log.debug("Installed version from registry: {}", version);
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

    /**
     * 获取已安装应用的安装路径
     *
     * @return 安装路径，如果未安装或查询失败则返回 null
     */
    public static String getInstallLocation() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", UNINSTALL_KEY_PATH, "/v", "InstallLocation"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("InstallLocation") && line.contains("REG_SZ")) {
                        String[] parts = line.trim().split("REG_SZ");
                        if (parts.length >= 2) {
                            String path = parts[1].trim();
                            log.debug("Install location from registry: {}", path);
                            return path;
                        }
                    }
                }
            }

            process.waitFor();

        } catch (Exception e) {
            log.warn("Failed to get install location: {}", e.getMessage());
        }

        return null;
    }
}


package com.laker.postman.service.update;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * JAR 更新管理器 - 负责下载、替换和重启 JAR 应用
 * 支持跨平台（Mac、Windows、Linux）
 */
@Slf4j
public class JarUpdateManager {

    private static final String JAR_EXTENSION = ".jar";
    private static final String OLD_JAR_SUFFIX = ".old";
    private static final String UPDATE_SCRIPT_NAME_WINDOWS = "update-restart.bat";
    private static final String UPDATE_SCRIPT_NAME_UNIX = "update-restart.sh";

    // 脚本模板路径
    private static final String WINDOWS_SCRIPT_TEMPLATE = "/update-scripts/update-restart-windows.bat";
    private static final String UNIX_SCRIPT_TEMPLATE = "/update-scripts/update-restart-unix.sh";
    private static final String MACOS_START_TEMPLATE = "/update-scripts/start-macos.sh";
    private static final String LINUX_START_TEMPLATE = "/update-scripts/start-linux.sh";

    /**
     * 下载并安装 JAR 更新
     *
     * @param downloadedJar 下载的新 JAR 文件
     * @return 是否成功启动更新和重启流程
     */
    public boolean installJarUpdate(File downloadedJar) {
        try {
            // 1. 获取当前运行的 JAR 文件路径
            File currentJar = getCurrentJarFile();
            if (currentJar == null) {
                log.error("Cannot determine current JAR file location");
                return false;
            }

            log.info("Current JAR: {}", currentJar.getAbsolutePath());
            log.info("Downloaded JAR: {}", downloadedJar.getAbsolutePath());

            // 2. 验证下载的 JAR 文件
            if (!isValidJarFile(downloadedJar)) {
                log.error("Downloaded file is not a valid JAR: {}", downloadedJar);
                return false;
            }

            // 3. 准备更新脚本
            File updateScript = prepareUpdateScript(currentJar, downloadedJar);
            if (updateScript == null) {
                log.error("Failed to create update script");
                return false;
            }

            // 4. 执行更新脚本并退出当前应用
            executeUpdateScript(updateScript);

            return true;
        } catch (Exception e) {
            log.error("Failed to install JAR update", e);
            return false;
        }
    }

    /**
     * 获取当前运行的 JAR 文件
     */
    private File getCurrentJarFile() {
        try {
            String jarPath = JarUpdateManager.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File jarFile = new File(jarPath);

            // 如果在 IDE 中运行（target/classes），返回 null
            if (jarFile.isDirectory()) {
                log.warn("Running from directory (IDE mode), not a JAR file");
                return null;
            }

            // 确保是 JAR 文件
            if (!jarFile.getName().endsWith(JAR_EXTENSION)) {
                log.warn("Current file is not a JAR: {}", jarFile.getName());
                return null;
            }

            return jarFile;
        } catch (Exception e) {
            log.error("Failed to get current JAR file", e);
            return null;
        }
    }

    /**
     * 验证 JAR 文件的有效性
     */
    private boolean isValidJarFile(File jarFile) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            return false;
        }

        // 简单验证：检查文件大小和扩展名
        if (jarFile.length() < 1024 || !jarFile.getName().endsWith(JAR_EXTENSION)) {
            return false;
        }

        // 可以进一步验证 JAR 文件结构（ZIP 格式）
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            // 检查是否包含 MANIFEST.MF
            return jar.getManifest() != null;
        } catch (IOException e) {
            log.warn("Invalid JAR file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 准备更新脚本（根据操作系统选择）
     */
    private File prepareUpdateScript(File currentJar, File downloadedJar) {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return createWindowsUpdateScript(currentJar, downloadedJar);
        } else {
            // macOS, Linux, Unix
            return createUnixUpdateScript(currentJar, downloadedJar);
        }
    }

    /**
     * 从资源文件加载脚本模板
     */
    private String loadScriptTemplate(String templatePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new IOException("Script template not found: " + templatePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 替换脚本中的占位符
     */
    private String replaceScriptPlaceholders(String script, Map<String, String> placeholders) {
        String result = script;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    /**
     * 创建 Windows 更新脚本（批处理文件）
     */
    private File createWindowsUpdateScript(File currentJar, File downloadedJar) {
        try {
            File scriptFile = new File(currentJar.getParentFile(), UPDATE_SCRIPT_NAME_WINDOWS);

            String currentJarPath = currentJar.getAbsolutePath();
            String downloadedJarPath = downloadedJar.getAbsolutePath();
            String backupJarPath = currentJarPath.replace(JAR_EXTENSION, OLD_JAR_SUFFIX + JAR_EXTENSION);

            // 加载脚本模板
            String scriptTemplate = loadScriptTemplate(WINDOWS_SCRIPT_TEMPLATE);

            // 替换占位符
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("CURRENT_JAR_PATH", currentJarPath);
            placeholders.put("DOWNLOADED_JAR_PATH", downloadedJarPath);
            placeholders.put("BACKUP_JAR_PATH", backupJarPath);

            String script = replaceScriptPlaceholders(scriptTemplate, placeholders);

            // 写入脚本文件（Windows 使用 GBK 编码）
            Files.write(scriptFile.toPath(), script.getBytes("GBK"));
            log.info("Created Windows update script: {}", scriptFile.getAbsolutePath());

            return scriptFile;
        } catch (Exception e) {
            log.error("Failed to create Windows update script", e);
            return null;
        }
    }

    /**
     * 创建 Unix/Mac 更新脚本（Shell 脚本）
     */
    private File createUnixUpdateScript(File currentJar, File downloadedJar) {
        try {
            File scriptFile = new File(currentJar.getParentFile(), UPDATE_SCRIPT_NAME_UNIX);

            String currentJarPath = currentJar.getAbsolutePath();
            String downloadedJarPath = downloadedJar.getAbsolutePath();
            String backupJarPath = currentJarPath.replace(JAR_EXTENSION, OLD_JAR_SUFFIX + JAR_EXTENSION);

            // 加载脚本模板
            String scriptTemplate = loadScriptTemplate(UNIX_SCRIPT_TEMPLATE);

            // 根据平台选择启动脚本
            String osName = System.getProperty("os.name").toLowerCase();
            String platformStartScript;
            if (osName.contains("mac")) {
                platformStartScript = loadScriptTemplate(MACOS_START_TEMPLATE);
            } else {
                platformStartScript = loadScriptTemplate(LINUX_START_TEMPLATE);
            }

            // 替换占位符
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("CURRENT_JAR_PATH", currentJarPath);
            placeholders.put("DOWNLOADED_JAR_PATH", downloadedJarPath);
            placeholders.put("BACKUP_JAR_PATH", backupJarPath);
            placeholders.put("SCRIPT_FILE_PATH", scriptFile.getAbsolutePath());
            placeholders.put("PLATFORM_SPECIFIC_START", platformStartScript);

            String script = replaceScriptPlaceholders(scriptTemplate, placeholders);
            script = replaceScriptPlaceholders(script, placeholders); // 二次替换，处理嵌套的占位符

            // 写入脚本文件
            Files.writeString(scriptFile.toPath(), script, StandardCharsets.UTF_8);

            // 设置执行权限
            if (!scriptFile.setExecutable(true, false)) {
                log.warn("Failed to set executable permission on script: {}", scriptFile.getAbsolutePath());
            }
            log.info("Created Unix update script: {}", scriptFile.getAbsolutePath());

            return scriptFile;
        } catch (Exception e) {
            log.error("Failed to create Unix update script", e);
            return null;
        }
    }

    /**
     * 执行更新脚本并退出当前应用
     */
    private void executeUpdateScript(File scriptFile) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (osName.contains("win")) {
                // Windows: 使用 cmd.exe 执行批处理文件
                pb = new ProcessBuilder("cmd.exe", "/c", "start", "/min", scriptFile.getAbsolutePath());
            } else {
                // Unix/Mac: 使用 nohup 和 bash 在后台执行脚本，脱离父进程
                pb = new ProcessBuilder("nohup", "/bin/bash", scriptFile.getAbsolutePath());

                // 重定向输出到日志文件（可选）
                File logFile = new File(scriptFile.getParentFile(), "update.log");
                pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));
                pb.redirectError(ProcessBuilder.Redirect.to(logFile));
            }

            // 设置工作目录
            pb.directory(scriptFile.getParentFile());

            // 启动脚本（不等待完成）
            Process process = pb.start();
            log.info("Update script started: {}", scriptFile.getAbsolutePath());

            // Unix/Mac: 确保脚本进程已启动
            if (!osName.contains("win")) {
                Thread.sleep(200);
                log.info("Update script process ID: {}", process.pid());
            }

            // 延迟退出，确保脚本已完全启动
            Thread.sleep(500);

            // 退出当前应用
            log.info("Exiting application for update...");
            System.exit(0);

        } catch (Exception e) {
            log.error("Failed to execute update script", e);
        }
    }
}
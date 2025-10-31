package com.laker.postman.service.update;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;

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
     * 创建 Windows 更新脚本（批处理文件）
     */
    private File createWindowsUpdateScript(File currentJar, File downloadedJar) {
        try {
            File scriptFile = new File(currentJar.getParentFile(), UPDATE_SCRIPT_NAME_WINDOWS);

            // 脚本逻辑：
            // 1. 等待当前进程结束
            // 2. 备份当前 JAR
            // 3. 用新 JAR 替换当前 JAR
            // 4. 启动新 JAR
            // 5. 清理备份和脚本

            String currentJarPath = currentJar.getAbsolutePath();
            String downloadedJarPath = downloadedJar.getAbsolutePath();
            String backupJarPath = currentJarPath.replace(JAR_EXTENSION, OLD_JAR_SUFFIX + JAR_EXTENSION);

            StringBuilder script = new StringBuilder();
            script.append("@echo off\n");
            script.append("chcp 65001 > nul\n");  // 设置 UTF-8 编码
            script.append("echo EasyPostman 正在更新...\n");
            script.append("echo Updating EasyPostman...\n\n");

            // 等待当前进程结束（最多等待 10 秒）
            script.append("set COUNTER=0\n");
            script.append(":WAIT_LOOP\n");
            script.append("timeout /t 1 /nobreak > nul\n");
            script.append("tasklist | find /i \"java.exe\" > nul\n");
            script.append("if errorlevel 1 goto UPDATE\n");
            script.append("set /a COUNTER+=1\n");
            script.append("if %COUNTER% lss 10 goto WAIT_LOOP\n\n");

            // 执行更新
            script.append(":UPDATE\n");
            script.append("echo 正在备份当前版本 / Backing up current version...\n");
            script.append("move /Y \"").append(currentJarPath).append("\" \"").append(backupJarPath).append("\" > nul 2>&1\n\n");

            script.append("echo 正在安装新版本 / Installing new version...\n");
            script.append("move /Y \"").append(downloadedJarPath).append("\" \"").append(currentJarPath).append("\"\n");
            script.append("if errorlevel 1 (\n");
            script.append("    echo 更新失败，正在恢复 / Update failed, restoring...\n");
            script.append("    move /Y \"").append(backupJarPath).append("\" \"").append(currentJarPath).append("\"\n");
            script.append("    goto END\n");
            script.append(")\n\n");

            // 启动新版本
            script.append("echo 正在启动新版本 / Starting new version...\n");
            script.append("start \"\" javaw -jar \"").append(currentJarPath).append("\"\n\n");

            // 清理
            script.append("timeout /t 2 /nobreak > nul\n");
            script.append("del /F /Q \"").append(backupJarPath).append("\" > nul 2>&1\n");
            script.append("del /F /Q \"%~f0\" > nul 2>&1\n\n");

            script.append(":END\n");
            script.append("exit\n");

            // 写入脚本文件
            Files.write(scriptFile.toPath(), script.toString().getBytes("GBK"));  // Windows 使用 GBK 编码
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

            StringBuilder script = new StringBuilder();
            script.append("#!/bin/bash\n\n");
            script.append("echo \"EasyPostman 正在更新...\"\n");
            script.append("echo \"Updating EasyPostman...\"\n\n");

            // 等待当前进程结束（更可靠的检测方式）
            script.append("# 等待当前进程结束\n");
            script.append("CURRENT_PID=$$\n");
            script.append("PARENT_PID=$PPID\n");
            script.append("echo \"Update script PID: $CURRENT_PID, Parent PID: $PARENT_PID\"\n\n");

            script.append("# 等待父进程结束（最多10秒）\n");
            script.append("for i in {1..20}; do\n");
            script.append("    if ! ps -p $PARENT_PID > /dev/null 2>&1; then\n");
            script.append("        echo \"Parent process ended\"\n");
            script.append("        break\n");
            script.append("    fi\n");
            script.append("    sleep 0.5\n");
            script.append("done\n\n");

            script.append("# 额外等待，确保所有资源释放\n");
            script.append("sleep 1\n\n");

            // 执行更新
            script.append("echo \"正在备份当前版本 / Backing up current version...\"\n");
            script.append("mv -f \"").append(currentJarPath).append("\" \"").append(backupJarPath).append("\" 2>/dev/null\n\n");

            script.append("echo \"正在安装新版本 / Installing new version...\"\n");
            script.append("mv -f \"").append(downloadedJarPath).append("\" \"").append(currentJarPath).append("\"\n");
            script.append("if [ $? -ne 0 ]; then\n");
            script.append("    echo \"更新失败，正在恢复 / Update failed, restoring...\"\n");
            script.append("    mv -f \"").append(backupJarPath).append("\" \"").append(currentJarPath).append("\"\n");
            script.append("    exit 1\n");
            script.append("fi\n\n");

            // 启动新版本
            script.append("echo \"正在启动新版本 / Starting new version...\"\n");

            // macOS 特殊处理：检查是否在 .app 包中运行
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("mac")) {
                script.append("# macOS: 检测是否在 .app 包中运行\n");
                script.append("if [[ \"").append(currentJarPath).append("\" == *\".app/Contents/\"* ]]; then\n");
                script.append("    # 在 .app 包中，需要重启整个应用\n");
                script.append("    APP_PATH=$(echo \"").append(currentJarPath).append("\" | sed 's|/Contents/.*||')\n");
                script.append("    echo \"Restarting app bundle: $APP_PATH\"\n");
                script.append("    open \"$APP_PATH\" &\n");
                script.append("    APP_STARTED=$?\n");
                script.append("else\n");
                script.append("    # 独立 JAR 文件，直接启动\n");
                script.append("    echo \"Starting standalone JAR\"\n");
                script.append("    nohup java -jar \"").append(currentJarPath).append("\" > /dev/null 2>&1 &\n");
                script.append("    APP_STARTED=$?\n");
                script.append("fi\n\n");
            } else {
                // Linux
                script.append("nohup java -jar \"").append(currentJarPath).append("\" > /dev/null 2>&1 &\n");
                script.append("APP_STARTED=$?\n\n");
            }

            // 验证启动
            script.append("if [ $APP_STARTED -eq 0 ]; then\n");
            script.append("    echo \"新版本已启动 / New version started successfully\"\n");
            script.append("else\n");
            script.append("    echo \"启动失败 / Failed to start new version\"\n");
            script.append("fi\n\n");

            // 清理
            script.append("sleep 2\n");
            script.append("rm -f \"").append(backupJarPath).append("\" 2>/dev/null\n");
            script.append("rm -f \"").append(scriptFile.getAbsolutePath()).append("\" 2>/dev/null\n\n");

            script.append("exit 0\n");

            // 写入脚本文件
            Files.writeString(scriptFile.toPath(), script.toString(), java.nio.charset.StandardCharsets.UTF_8);

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

    /**
     * 检查是否正在从 JAR 运行（而不是 IDE）
     */
    public static boolean isRunningFromJar() {
        try {
            String jarPath = JarUpdateManager.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File jarFile = new File(jarPath);
            return jarFile.isFile() && jarFile.getName().endsWith(JAR_EXTENSION);
        } catch (Exception e) {
            return false;
        }
    }
}


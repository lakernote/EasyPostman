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
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 *   <li>验证下载的 JAR 文件有效性</li>
 *   <li>根据操作系统生成更新脚本（Windows .bat / Unix .sh）</li>
 *   <li>执行更新脚本并退出当前应用</li>
 *   <li>脚本自动完成：备份 → 替换 → 重启 → 清理</li>
 * </ul>
 *
 * <p><b>支持平台：</b></p>
 * <ul>
 *   <li>✅ Windows (批处理脚本)</li>
 *   <li>✅ macOS (Shell 脚本，支持 .app 包和独立 JAR)</li>
 *   <li>✅ Linux (Shell 脚本)</li>
 * </ul>
 *
 * <p><b>更新流程：</b></p>
 * <pre>
 * 1. 下载新 JAR 文件
 * 2. 验证 JAR 有效性（检查文件格式和 MANIFEST）
 * 3. 生成更新脚本（从模板替换路径占位符）
 * 4. 启动更新脚本
 * 5. 当前应用退出
 * 6. 脚本等待父进程结束
 * 7. 脚本备份当前 JAR（添加 .old 后缀）
 * 8. 脚本用新 JAR 替换当前 JAR
 * 9. 脚本启动新版本应用
 * 10. 脚本清理备份文件和自身
 * </pre>
 *
 * <p><b>脚本日志位置：</b></p>
 * <table border="1">
 *   <tr><th>平台</th><th>日志位置</th><th>说明</th></tr>
 *   <tr>
 *     <td>Windows</td>
 *     <td>无单独日志文件</td>
 *     <td>脚本输出到控制台窗口（最小化运行）</td>
 *   </tr>
 *   <tr>
 *     <td>macOS</td>
 *     <td><code>&lt;JAR目录&gt;/update.log</code></td>
 *     <td>例如：<code>/Applications/EasyPostman.app/Contents/app/update.log</code></td>
 *   </tr>
 *   <tr>
 *     <td>Linux</td>
 *     <td><code>&lt;JAR目录&gt;/update.log</code></td>
 *     <td>例如：<code>/opt/easypostman/update.log</code></td>
 *   </tr>
 * </table>
 *
 * <p><b>错误处理：</b></p>
 * <ul>
 *   <li>更新失败时自动恢复备份（.old 文件）</li>
 *   <li>脚本执行失败时保留备份文件供手动恢复</li>
 *   <li>所有关键操作都有日志记录</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * JarUpdateManager manager = new JarUpdateManager();
 * File downloadedJar = new File("/tmp/easy-postman-3.1.7.jar");
 * boolean success = manager.installJarUpdate(downloadedJar);
 * // 如果成功，应用会自动退出并重启到新版本
 * }</pre>
 *
 * @author GitHub Copilot
 * @since 3.1.7
 * @see UpdateDownloader
 */
@Slf4j
public class JarUpdateManager {

    // ==================== 常量定义 ====================

    /** JAR 文件扩展名 */
    private static final String JAR_EXTENSION = ".jar";

    /** 备份 JAR 文件的后缀（会在原文件名后添加，如 app.jar.old） */
    private static final String OLD_JAR_SUFFIX = ".old";

    /** Windows 更新脚本文件名 */
    private static final String UPDATE_SCRIPT_NAME_WINDOWS = "update-restart.bat";

    /** Unix/Mac 更新脚本文件名 */
    private static final String UPDATE_SCRIPT_NAME_UNIX = "update-restart.sh";

    /** 更新日志文件名（仅 Unix/Mac 使用） */
    private static final String UPDATE_LOG_NAME = "update.log";

    // ==================== 脚本模板路径（resources 目录） ====================

    /** Windows 更新脚本模板路径（包含完整的批处理逻辑） */
    private static final String WINDOWS_SCRIPT_TEMPLATE = "/update-scripts/update-restart-windows.bat";

    /** Unix/Mac 更新脚本模板路径（包含通用的 Shell 逻辑） */
    private static final String UNIX_SCRIPT_TEMPLATE = "/update-scripts/update-restart-unix.sh";

    /** macOS 特定启动脚本模板（处理 .app 包和独立 JAR） */
    private static final String MACOS_START_TEMPLATE = "/update-scripts/start-macos.sh";

    /** Linux 启动脚本模板（直接启动 JAR） */
    private static final String LINUX_START_TEMPLATE = "/update-scripts/start-linux.sh";

    /**
     * 下载并安装 JAR 更新
     *
     * <p>此方法会：</p>
     * <ol>
     *   <li>获取当前运行的 JAR 文件路径</li>
     *   <li>验证下载的 JAR 文件有效性</li>
     *   <li>根据操作系统生成更新脚本</li>
     *   <li>启动更新脚本并退出当前应用</li>
     * </ol>
     *
     * <p><b>注意：</b>如果方法返回 {@code true}，当前应用会立即退出（调用 {@code System.exit(0)}）</p>
     *
     * @param downloadedJar 下载的新 JAR 文件（必须是有效的 JAR 文件）
     * @return {@code true} 如果成功启动更新流程，{@code false} 如果失败
     *         注意：返回 {@code true} 后应用会自动退出
     *
     * @throws IllegalArgumentException 如果 downloadedJar 为 null
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
     *
     * <p>通过 ProtectionDomain 获取当前类的代码源位置，判断是否从 JAR 运行。</p>
     *
     * <p><b>检测逻辑：</b></p>
     * <ul>
     *   <li>如果是目录（如 target/classes）→ 返回 null（IDE 运行）</li>
     *   <li>如果不是 .jar 文件 → 返回 null</li>
     *   <li>否则返回 JAR 文件路径</li>
     * </ul>
     *
     * @return 当前 JAR 文件，如果不是从 JAR 运行则返回 {@code null}
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
     *
     * <p>验证步骤：</p>
     * <ol>
     *   <li>检查文件是否存在且是文件（不是目录）</li>
     *   <li>检查文件大小（至少 1KB）</li>
     *   <li>检查文件扩展名是否为 .jar</li>
     *   <li>尝试打开 JAR 文件并读取 MANIFEST.MF</li>
     * </ol>
     *
     * @param jarFile 要验证的 JAR 文件
     * @return {@code true} 如果是有效的 JAR 文件，{@code false} 否则
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
     *
     * <p>根据 {@code os.name} 系统属性判断操作系统类型：</p>
     * <ul>
     *   <li>Windows → 生成 .bat 批处理脚本</li>
     *   <li>其他（macOS/Linux/Unix）→ 生成 .sh Shell 脚本</li>
     * </ul>
     *
     * @param currentJar 当前运行的 JAR 文件
     * @param downloadedJar 下载的新 JAR 文件
     * @return 生成的更新脚本文件，失败返回 {@code null}
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
     *
     * <p>从 classpath 的 resources 目录加载脚本模板文件。</p>
     * <p>模板文件使用 {{PLACEHOLDER}} 格式的占位符，后续会被实际路径替换。</p>
     *
     * @param templatePath 模板文件路径（相对于 resources 目录，如 "/update-scripts/xxx.bat"）
     * @return 模板文件内容（UTF-8 编码）
     * @throws IOException 如果模板文件不存在或读取失败
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
     *
     * <p>将脚本中的 {{KEY}} 占位符替换为实际值。</p>
     *
     * <p><b>常用占位符：</b></p>
     * <ul>
     *   <li>{{CURRENT_JAR_PATH}} - 当前 JAR 文件的绝对路径</li>
     *   <li>{{DOWNLOADED_JAR_PATH}} - 下载的新 JAR 文件的绝对路径</li>
     *   <li>{{BACKUP_JAR_PATH}} - 备份文件路径（当前 JAR + .old 后缀）</li>
     *   <li>{{SCRIPT_FILE_PATH}} - 脚本自身的路径（用于自删除）</li>
     *   <li>{{PLATFORM_SPECIFIC_START}} - 平台特定的启动脚本</li>
     * </ul>
     *
     * @param script 原始脚本内容
     * @param placeholders 占位符映射（key 不需要包含 {{ }}）
     * @return 替换后的脚本内容
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
     *
     * <p><b>脚本功能：</b></p>
     * <ol>
     *   <li>等待当前 Java 进程结束（最多 10 秒）</li>
     *   <li>备份当前 JAR（移动为 .old 文件）</li>
     *   <li>用新 JAR 替换当前 JAR</li>
     *   <li>使用 javaw 启动新版本（不显示控制台窗口）</li>
     *   <li>等待 2 秒后清理备份文件和脚本自身</li>
     * </ol>
     *
     * <p><b>编码：</b>使用 GBK 编码以支持中文路径和输出</p>
     *
     * <p><b>日志：</b>输出到控制台（最小化窗口运行）</p>
     *
     * <p><b>错误处理：</b>如果更新失败，自动恢复备份文件</p>
     *
     * @param currentJar 当前运行的 JAR 文件
     * @param downloadedJar 下载的新 JAR 文件
     * @return 生成的批处理脚本文件，失败返回 {@code null}
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
     *
     * <p><b>脚本功能：</b></p>
     * <ol>
     *   <li>等待父进程（当前应用）结束（通过 PPID，最多 10 秒）</li>
     *   <li>额外等待 1 秒确保资源释放</li>
     *   <li>备份当前 JAR（移动为 .old 文件）</li>
     *   <li>用新 JAR 替换当前 JAR</li>
     *   <li>根据平台启动新版本：
     *     <ul>
     *       <li>macOS .app 包：使用 {@code open} 命令重启整个应用</li>
     *       <li>独立 JAR：使用 {@code nohup java -jar} 后台启动</li>
     *     </ul>
     *   </li>
     *   <li>等待 2 秒后清理备份文件和脚本自身</li>
     * </ol>
     *
     * <p><b>编码：</b>UTF-8</p>
     *
     * <p><b>日志：</b>输出到 {@code <JAR目录>/update.log}</p>
     * <p>例如：{@code /Applications/EasyPostman.app/Contents/app/update.log}</p>
     *
     * <p><b>权限：</b>自动设置为可执行（chmod +x）</p>
     *
     * <p><b>错误处理：</b>如果更新失败，自动恢复备份文件并退出</p>
     *
     * @param currentJar 当前运行的 JAR 文件
     * @param downloadedJar 下载的新 JAR 文件
     * @return 生成的 Shell 脚本文件，失败返回 {@code null}
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
     *
     * <p><b>执行方式：</b></p>
     * <ul>
     *   <li>Windows: {@code cmd.exe /c start /min <script>} - 最小化窗口启动</li>
     *   <li>Unix/Mac: {@code nohup /bin/bash <script>} - 后台运行，输出重定向到日志</li>
     * </ul>
     *
     * <p><b>退出流程：</b></p>
     * <ol>
     *   <li>启动更新脚本进程（不等待完成）</li>
     *   <li>等待 200ms 确保脚本已启动（Unix/Mac）</li>
     *   <li>等待 500ms 确保脚本完全启动</li>
     *   <li>调用 {@code System.exit(0)} 退出当前应用</li>
     * </ol>
     *
     * <p><b>注意：</b>此方法调用后应用会立即退出！</p>
     *
     * @param scriptFile 要执行的更新脚本文件
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
                File logFile = new File(scriptFile.getParentFile(), UPDATE_LOG_NAME);
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
            // Windows需要更长时间确保文件句柄释放
            if (osName.contains("win")) {
                Thread.sleep(1000);
            } else {
                Thread.sleep(500);
            }

            // 退出当前应用
            log.info("Exiting application for update...");
            System.exit(0);

        } catch (Exception e) {
            log.error("Failed to execute update script", e);
        }
    }

}
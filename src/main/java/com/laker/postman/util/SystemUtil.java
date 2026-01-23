package com.laker.postman.util;

import com.laker.postman.model.Workspace;
import com.laker.postman.service.update.asset.WindowsVersionDetector;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@UtilityClass
public class SystemUtil {
    /**
     * 系统信息
     */
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String OS_ARCH = System.getProperty("os.arch");
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String PATH_SEPARATOR = File.pathSeparator;

    /**
     * 缓存的数据目录路径
     */
    private static String cachedDataPath = null;


    /**
     * 获取程序所在目录
     */
    private static String getApplicationDirectory() {
        try {
            String path = SystemUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File jarFile = new File(decodedPath);

            // 如果是 jar 文件，获取其父目录
            if (jarFile.isFile()) {
                return jarFile.getParent() + File.separator;
            } else {
                // 开发环境，使用项目根目录
                return System.getProperty("user.dir") + File.separator;
            }
        } catch (Exception e) {
            log.warn("无法获取应用程序目录，使用当前工作目录", e);
            return System.getProperty("user.dir") + File.separator;
        }
    }

    /**
     * 检查是否为 Portable 模式
     * 复用 WindowsVersionDetector 的检测逻辑
     */
    private static boolean isPortableMode() {
        return WindowsVersionDetector.isPortableVersion();
    }

    /**
     * 获取数据存储根目录
     * Portable 模式：程序所在目录/data/
     * 普通模式：用户主目录/EasyPostman/
     */
    public static String getEasyPostmanPath() {
        if (cachedDataPath != null) {
            return cachedDataPath;
        }

        String dataPath;
        if (isPortableMode()) {
            // Portable 模式：使用程序所在目录
            String appDir = getApplicationDirectory();
            dataPath = appDir + "data" + File.separator;
            log.info("运行在 Portable 模式，数据目录: {}", dataPath);
        } else {
            // 普通模式：使用用户主目录
            dataPath = System.getProperty("user.home") + File.separator + "EasyPostman" + File.separator;
        }

        // 确保目录存在
        try {
            File dataDir = new File(dataPath);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                log.info("创建数据目录: {}", dataPath);
            }
        } catch (Exception e) {
            log.error("创建数据目录失败: {}", dataPath, e);
        }

        cachedDataPath = dataPath;
        return dataPath;
    }

    /**
     * 获取当前版本号：优先从 MANIFEST.MF Implementation-Version，若无则尝试读取 pom.xml
     */
    public static String getCurrentVersion() {
        String version = null;
        try {
            version = SystemUtil.class.getPackage().getImplementationVersion();
        } catch (Exception ignored) {
            // 忽略异常，可能是没有 MANIFEST.MF 文件
        }
        if (version != null && !version.isBlank()) {
            return "v" + version;
        }
        // 开发环境下读取 pom.xml
        try {
            Path pom = Paths.get("pom.xml");
            if (Files.exists(pom)) {
                String xml = Files.readString(pom);
                int idx = xml.indexOf("<version>");
                if (idx > 0) {
                    int start = idx + "<version>".length();
                    int end = xml.indexOf("</version>", start);
                    if (end > start) {
                        version = xml.substring(start, end).trim();
                        return "v" + version;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "开发环境";
    }


    /**
     * 检查剪贴板是否有cURL命令，有则返回文本，否则返回null
     */
    public static String getClipboardCurlText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = clipboard.getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                if (text.trim().toLowerCase().startsWith("curl")) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.warn("获取剪贴板cURL文本失败", e);
        }
        return null;
    }

    public static String getEnvPathForWorkspace(Workspace ws) {
        return ConfigPathConstants.getEnvironmentsPath(ws);
    }

    public static String getCollectionPathForWorkspace(Workspace ws) {
        return ConfigPathConstants.getCollectionsPath(ws);
    }

    /**
     * 获取操作系统信息
     */
    public static String getOsInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS Name:        ").append(OS_NAME).append('\n');
        sb.append("OS Arch:        ").append(OS_ARCH).append('\n');
        sb.append("OS Version:     ").append(OS_VERSION).append('\n');
        sb.append("Path Separator: ").append(PATH_SEPARATOR).append('\n');
        sb.append("File Separator: ").append(FILE_SEPARATOR).append('\n');
        sb.append("Line Separator: ").append(LINE_SEPARATOR);
        return sb.toString();
    }

}
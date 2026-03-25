package com.laker.postman.util;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.Workspace;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
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
        return AppRuntimeLayout.applicationRootDirectory(SystemUtil.class).toString() + File.separator;
    }

    private static boolean isPortableMode() {
        return AppRuntimeLayout.isPortableMode(SystemUtil.class);
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
        String override = System.getProperty("easyPostman.data.dir");
        if (override != null && !override.isBlank()) {
            dataPath = Paths.get(override.trim()).toAbsolutePath().normalize().toString() + File.separator;
        } else {
            dataPath = AppRuntimeLayout.dataRootDirectory(SystemUtil.class).toString() + File.separator;
            if (isPortableMode()) {
                log.info("运行在 Portable 模式，数据目录: {}", dataPath);
            }
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
     * 仅供测试使用，清理静态缓存。
     */
    public static void resetForTests() {
        cachedDataPath = null;
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
        try {
            version = resolveVersionFromPom(Paths.get("pom.xml"));
            if (version != null && !version.isBlank()) {
                return "v" + version;
            }
        } catch (Exception ignored) {
        }
        return "开发环境";
    }

    private static String resolveVersionFromPom(Path pom) {
        Path current = pom;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            if (Files.exists(current)) {
                String xml = readPom(current);
                String version = readXmlTag(xml, "version");
                String resolved = resolveVersionToken(version, xml, current);
                if (resolved != null && !resolved.contains("${")) {
                    return resolved;
                }
                String revision = readXmlTag(xml, "revision");
                if (revision != null && !revision.isBlank()) {
                    return revision.trim();
                }
                String relativePath = readXmlTag(xml, "relativePath");
                if (relativePath != null && !relativePath.isBlank()) {
                    current = current.getParent().resolve(relativePath.trim()).normalize();
                    continue;
                }
            }
            current = current.getParent() == null ? null : current.getParent().getParent() == null ? null : current.getParent().getParent().resolve("pom.xml");
        }
        return null;
    }

    private static String resolveVersionToken(String version, String xml, Path currentPom) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.trim();
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return trimmed;
        }
        String propertyName = trimmed.substring(2, trimmed.length() - 1);
        String propertyValue = readXmlTag(xml, propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        if ("revision".equals(propertyName)) {
            Path parentPom = currentPom.getParent() == null ? null : currentPom.getParent().getParent();
            if (parentPom != null) {
                return resolveVersionFromPom(parentPom.resolve("pom.xml"));
            }
        }
        return trimmed;
    }

    private static String readPom(Path pom) {
        try {
            return Files.readString(pom);
        } catch (Exception e) {
            return "";
        }
    }

    private static String readXmlTag(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf(openTag);
        if (start < 0) {
            return null;
        }
        int contentStart = start + openTag.length();
        int end = xml.indexOf(closeTag, contentStart);
        if (end <= contentStart) {
            return null;
        }
        return xml.substring(contentStart, end).trim();
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

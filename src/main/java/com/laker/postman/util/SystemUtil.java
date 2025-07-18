package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
public class SystemUtil {
    public static final String LOG_DIR = getUserHomeEasyPostmanPath() + "logs" + File.separator;
    public static final String COLLECTION_PATH = getUserHomeEasyPostmanPath() + "collections.json";
    public static final String ENV_PATH = getUserHomeEasyPostmanPath() + "environments.json";

    public static String getUserHomeEasyPostmanPath() {
        return System.getProperty("user.home") + File.separator + "EasyPostman" + File.separator;
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
                String xml = java.nio.file.Files.readString(pom);
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
}
package com.laker.postman.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


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
}
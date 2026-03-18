package com.laker.postman.plugin.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 运行时版本解析器。
 * <p>
 * 负责解析当前 app 版本和插件平台版本，并在开发态提供 pom 回退读取能力。
 * </p>
 */
final class RuntimeVersionResolver {

    private RuntimeVersionResolver() {
    }

    static String resolveCurrentAppVersion(Class<?> anchorClass) {
        String implementationVersion = anchorClass.getPackage().getImplementationVersion();
        if (implementationVersion != null && !implementationVersion.isBlank()) {
            return implementationVersion;
        }
        String pomVersion = resolvePomVersion(Paths.get("pom.xml"));
        return pomVersion == null || pomVersion.isBlank() ? "dev" : pomVersion;
    }

    static String resolveCurrentPlatformVersion(Class<?> anchorClass,
                                                String resourcePath,
                                                String versionKey) {
        try (InputStream inputStream = anchorClass.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                String propertyVersion = properties.getProperty(versionKey);
                if (propertyVersion != null && !propertyVersion.isBlank()) {
                    return propertyVersion.trim();
                }
            }
        } catch (IOException ignored) {
        }

        String pomValue = resolvePomProperty(Paths.get("pom.xml"), versionKey);
        return pomValue == null || pomValue.isBlank() ? "dev" : pomValue;
    }

    private static String resolvePomVersion(Path pom) {
        String version = resolvePomProperty(pom, "version");
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.trim();
        if ("${revision}".equals(trimmed)) {
            return resolvePomProperty(pom, "revision");
        }
        return trimmed.contains("${") ? null : trimmed;
    }

    private static String resolvePomProperty(Path pom, String propertyName) {
        Path current = pom;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            try {
                if (Files.exists(current)) {
                    String xml = Files.readString(current);
                    String property = readXmlTag(xml, propertyName);
                    if (property != null && !property.isBlank()) {
                        return property.trim();
                    }
                    String relativePath = readXmlTag(xml, "relativePath");
                    if (relativePath != null && !relativePath.isBlank()) {
                        current = current.getParent().resolve(relativePath.trim()).normalize();
                        continue;
                    }
                }
            } catch (Exception ignored) {
            }
            current = current.getParent() == null ? null : current.getParent().getParent() == null ? null : current.getParent().getParent().resolve("pom.xml");
        }
        return null;
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
}

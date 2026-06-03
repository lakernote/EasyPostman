package com.laker.postman.util;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.io.IOException;
import java.io.InputStream;

/**
 * 编辑器主题工具类
 * 为 RSyntaxTextArea 提供主题适配支持，根据 FlatLaf 主题自动选择亮色或暗色编辑器主题
 */
@Slf4j
@UtilityClass
public class EditorThemeUtil {

    private static volatile String configuredThemeResourcePath;
    private static volatile String configuredFallbackThemeResourcePath;

    /**
     * 加载并应用编辑器主题 - 支持亮色和暗色主题自适应
     *
     * @param area RSyntaxTextArea 编辑器实例
     */
    public static void loadTheme(RSyntaxTextArea area) {
        String themeFile = themeResourcePath();
        InputStream in = null;
        in = loadResource(themeFile);
        if (in == null) {
            themeFile = fallbackThemeResourcePath();
            in = loadResource(themeFile);
        }
        try {
            if (in != null) {
                Theme theme = Theme.load(in);
                theme.apply(area);
                log.debug("Loaded RSyntaxTextArea theme: {}", themeFile);
            } else {
                log.warn("Theme file not found: {}", themeFile);
            }
        } catch (IOException e) {
            log.error("Failed to load editor theme: {}", themeFile, e);
        }
    }

    public static void configureThemeResources(String themeResourcePath, String fallbackThemeResourcePath) {
        configuredThemeResourcePath = normalizeConfiguredPath(themeResourcePath);
        configuredFallbackThemeResourcePath = normalizeConfiguredPath(fallbackThemeResourcePath);
    }

    public static void clearConfiguredThemeResources() {
        configuredThemeResourcePath = null;
        configuredFallbackThemeResourcePath = null;
    }

    static String themeResourcePath() {
        if (configuredThemeResourcePath != null) {
            return configuredThemeResourcePath;
        }
        return ModernColors.isDarkTheme()
                ? "/themes/easypostman-dark.xml"
                : "/themes/easypostman-light.xml";
    }

    private static String fallbackThemeResourcePath() {
        if (configuredFallbackThemeResourcePath != null) {
            return configuredFallbackThemeResourcePath;
        }
        return ModernColors.isDarkTheme()
                ? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                : "/org/fife/ui/rsyntaxtextarea/themes/vs.xml";
    }

    private static String normalizeConfiguredPath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        String trimmed = resourcePath.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static InputStream loadResource(String resourcePath) {
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream contextStream = contextClassLoader.getResourceAsStream(normalized);
            if (contextStream != null) {
                return contextStream;
            }
        }
        InputStream localStream = EditorThemeUtil.class.getResourceAsStream(resourcePath);
        if (localStream != null) {
            return localStream;
        }
        return EditorThemeUtil.class.getClassLoader().getResourceAsStream(normalized);
    }
}

package com.laker.postman.util;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/**
 * 编辑器主题工具类
 * 为 RSyntaxTextArea 提供主题适配支持，根据 FlatLaf 主题自动选择亮色或暗色编辑器主题
 */
@Slf4j
@UtilityClass
public class EditorThemeUtil {

    private static final Color DARK_EDITOR_BACKGROUND = new Color(0x3A, 0x3D, 0x3F);
    private static final Color DARK_GUTTER_BACKGROUND = new Color(0x38, 0x3B, 0x3D);
    private static final Color DARK_EDITOR_DIVIDER = new Color(0x45, 0x49, 0x4C);
    private static final Color DARK_LINE_NUMBER = new Color(0x74, 0x7A, 0x82);
    private static final Color DARK_CURRENT_LINE_NUMBER = new Color(0xA9, 0xB7, 0xC6);
    private static final Color LIGHT_EDITOR_BACKGROUND = Color.WHITE;
    private static final Color LIGHT_GUTTER_BACKGROUND = Color.WHITE;
    private static final Color LIGHT_EDITOR_DIVIDER = new Color(0xE0, 0xE0, 0xE0);
    private static final Color LIGHT_LINE_NUMBER = new Color(0x99, 0x99, 0x99);
    private static final Color LIGHT_CURRENT_LINE_NUMBER = new Color(0x66, 0x66, 0x66);

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

    /**
     * Aligns RTextScrollPane chrome with the active editor theme.
     * <p>
     * RSyntaxTextArea themes style the editor and gutter, but scroll panes created after
     * theme application can keep default gutter/border colors. Keep this in the shared
     * editor utility so request, response, script, and toolbox editors stay consistent.
     */
    public static void applyScrollPaneChrome(RTextScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }

        Color editorBackground = ModernColors.isDarkTheme() ? DARK_EDITOR_BACKGROUND : LIGHT_EDITOR_BACKGROUND;
        Color gutterBackground = ModernColors.isDarkTheme() ? DARK_GUTTER_BACKGROUND : LIGHT_GUTTER_BACKGROUND;
        Color divider = ModernColors.isDarkTheme() ? DARK_EDITOR_DIVIDER : LIGHT_EDITOR_DIVIDER;
        Color lineNumber = ModernColors.isDarkTheme() ? DARK_LINE_NUMBER : LIGHT_LINE_NUMBER;
        Color currentLineNumber = ModernColors.isDarkTheme()
                ? DARK_CURRENT_LINE_NUMBER
                : LIGHT_CURRENT_LINE_NUMBER;

        scrollPane.setBackground(editorBackground);
        scrollPane.getViewport().setBackground(editorBackground);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        applyEditorScrollBar(scrollPane.getVerticalScrollBar(), editorBackground);
        applyEditorScrollBar(scrollPane.getHorizontalScrollBar(), editorBackground);

        Gutter gutter = scrollPane.getGutter();
        if (gutter == null) {
            return;
        }
        gutter.setBackground(gutterBackground);
        gutter.setBorderColor(divider);
        gutter.setLineNumberColor(lineNumber);
        gutter.setCurrentLineNumberColor(currentLineNumber);
        gutter.setFoldBackground(gutterBackground);
        gutter.setArmedFoldBackground(divider);
        gutter.setFoldIndicatorForeground(lineNumber);
        gutter.setFoldIndicatorArmedForeground(currentLineNumber);
    }

    private static void applyEditorScrollBar(JScrollBar scrollBar, Color background) {
        if (scrollBar == null) {
            return;
        }
        scrollBar.setOpaque(true);
        scrollBar.setBackground(background);
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

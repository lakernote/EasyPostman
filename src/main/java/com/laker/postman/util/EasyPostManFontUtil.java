package com.laker.postman.util;

import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 字体工具类，统一根据操作系统选择合适的字体。
 */
public class EasyPostManFontUtil {
    public static final int DEFAULT_FONT_SIZE = 12;

    // 默认字体名称常量
    private static final String DEFAULT_FONT_NAME = "SansSerif";

    // 缓存首选字体族，避免重复查找
    private static String cachedPreferredFontFamily = null;

    // 私有构造函数，防止实例化工具类
    private EasyPostManFontUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取默认字体，保留字体降级链以支持 emoji 等特殊字符
     *
     * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param size  字体大小
     * @return Font 对象，使用首选字体族
     */
    public static Font getDefaultFont(int style, int size) {
        String fontFamily = getCachedPreferredFontFamily();
        return new Font(fontFamily, style, size);
    }

    /**
     * 获取缓存的首选字体族，如果未缓存则查找并缓存
     *
     * @return 首选字体族名称
     */
    private static String getCachedPreferredFontFamily() {
        if (cachedPreferredFontFamily == null) {
            cachedPreferredFontFamily = getPreferredFontFamily();
            if (cachedPreferredFontFamily == null) {
                cachedPreferredFontFamily = DEFAULT_FONT_NAME;
            }
        }
        return cachedPreferredFontFamily;
    }

    /**
     * 设置字体缩放和首选字体族，保留字体降级链，支持 emoji 等特殊字符
     * 这种方式不会破坏系统的字体 fallback 机制
     */
    public static void setupFontScaling() {

        // 根据操作系统设置首选字体族，而不是直接替换字体
        String preferredFontFamily = getPreferredFontFamily();

        if (preferredFontFamily != null && !DEFAULT_FONT_NAME.equals(preferredFontFamily)) {
            // 仅设置默认字体族，让系统自动处理降级
            UIManager.put("defaultFont", new Font(preferredFontFamily, Font.PLAIN, DEFAULT_FONT_SIZE));

            // 针对特定组件设置字体族（而非完整字体），保留降级链
            Font baseFont = new Font(preferredFontFamily, Font.PLAIN, DEFAULT_FONT_SIZE);

            // 只设置关键的 UI 组件字体，使用 deriveFont 保留字体属性
            UIManager.put("Label.font", baseFont);
            UIManager.put("Button.font", baseFont);
            UIManager.put("TextField.font", baseFont);
            UIManager.put("TextArea.font", baseFont);
            UIManager.put("ComboBox.font", baseFont);
            UIManager.put("Table.font", baseFont);
            UIManager.put("Tree.font", baseFont);
            UIManager.put("List.font", baseFont);
            UIManager.put("Menu.font", baseFont);
            UIManager.put("MenuItem.font", baseFont);
            UIManager.put("PopupMenu.font", baseFont);
            UIManager.put("ToolTip.font", baseFont);
            UIManager.put("TabbedPane.font", baseFont);
        }
    }

    /**
     * 获取操作系统首选字体族名称
     *
     * @return 首选字体族名称，如果没有找到则返回 null
     */
    private static String getPreferredFontFamily() {
        if (SystemInfo.isMacOS) {
            // macOS 优先使用 PingFang SC
            String[] macFonts = {"PingFang SC", ".AppleSystemUIFont", "Hiragino Sans GB"};
            return findAvailableFontFamily(macFonts);
        } else if (SystemInfo.isWindows) {
            // Windows 优先使用 Microsoft YaHei UI 或 Segoe UI
            String[] winFonts = {"Microsoft YaHei UI", "微软雅黑", "Segoe UI"};
            return findAvailableFontFamily(winFonts);
        } else if (SystemInfo.isLinux) {
            // Linux 优先使用 Noto Sans 或 WenQuanYi
            String[] linuxFonts = {"Noto Sans CJK SC", "WenQuanYi Micro Hei", "DejaVu Sans"};
            return findAvailableFontFamily(linuxFonts);
        }
        return null;
    }

    /**
     * 从字体列表中查找第一个可用的字体族
     *
     * @param fontNames 字体名称数组
     * @return 第一个可用的字体族名称，如果都不可用则返回 null
     */
    private static String findAvailableFontFamily(String[] fontNames) {
        if (fontNames == null) {
            return null;
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();
        Set<String> availableFontSet = new HashSet<>(Arrays.asList(availableFonts));

        for (String fontName : fontNames) {
            if (fontName != null && availableFontSet.contains(fontName)) {
                return fontName;
            }
        }
        return null;
    }
}
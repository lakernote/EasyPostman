package com.laker.postman.util;

import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * 字体工具类，统一根据操作系统选择合适的字体。
 */
public class EasyPostManFontUtil {
    public static final int DEFAULT_FONT_SIZE = 12;

    // 默认字体名称常量
    private static final String DEFAULT_FONT_NAME = "SansSerif";

    // 缓存需要设置字体的 UI 属性键，避免每次都遍历所有键
    private static final Set<String> FONT_UI_KEYS = new HashSet<>(Arrays.asList(
            "Menu.font", "MenuItem.font", "MenuBar.font", "PopupMenu.font",
            "ToolTip.font", "Button.font", "Label.font", "TextField.font",
            "TextArea.font", "CheckBoxMenuItem.font", "RadioButtonMenuItem.font",
            "OptionPane.font", "Tree.font", "Table.font", "List.font",
            "ComboBox.font", "TabbedPane.font", "Panel.font", "ScrollPane.font",
            "CheckBox.font", "RadioButton.font", "ToggleButton.font",
            "ProgressBar.font", "Slider.font", "Spinner.font", "EditorPane.font",
            "TextPane.font", "PasswordField.font", "FormattedTextField.font"
    ));

    // 私有构造函数，防止实例化工具类
    private EasyPostManFontUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取适合当前操作系统的默认字体
     *
     * @param style 字体样式 Font.PLAIN | Font.BOLD | Font.ITALIC | Font.BOLD + Font.ITALIC
     * @param size  字体大小 数值越大字体越大
     * @return Font
     */
    public static Font getDefaultFont(int style, int size) {
        // 参数校验
        if (size <= 0) {
            throw new IllegalArgumentException("字体大小必须大于 0");
        }

        // 验证字体样式参数
        if (style < 0 || style > (Font.BOLD | Font.ITALIC)) {
            throw new IllegalArgumentException("无效的字体样式，应为 Font.PLAIN、Font.BOLD、Font.ITALIC 或其组合");
        }

        if (SystemInfo.isMacOS) {
            // 优先 PingFang SC，找不到则降级
            String[] macFonts = {"PingFang SC", "苹方-简", "Hiragino Sans GB", "Heiti SC", "微软雅黑", DEFAULT_FONT_NAME};
            Font font = findAvailableFont(style, size, macFonts);
            if (font != null) return font;
        } else if (SystemInfo.isWindows) {
            // 优先 Microsoft YaHei UI，其次微软雅黑、Segoe UI，找不到则降级
            String[] winFonts = {"Microsoft YaHei UI", "微软雅黑", "Segoe UI", "宋体", DEFAULT_FONT_NAME};
            Font font = findAvailableFont(style, size, winFonts);
            if (font != null) return font;
        } else if (SystemInfo.isLinux) {
            // Linux 系统字体支持
            String[] linuxFonts = {"Noto Sans CJK SC", "WenQuanYi Micro Hei", "文泉驿微米黑",
                    "DejaVu Sans", "Liberation Sans", DEFAULT_FONT_NAME};
            Font font = findAvailableFont(style, size, linuxFonts);
            if (font != null) return font;
        }
        return new Font(DEFAULT_FONT_NAME, style, size);
    }

    /**
     * 设置全局字体，覆盖所有 UI 组件的默认字体
     *
     * @param font 要设置的字体，不能为 null
     */
    public static void setupGlobalFont(Font font) {
        if (font == null) {
            throw new IllegalArgumentException("字体不能为 null");
        }

        // 优化：只遍历 FontUIResource 类型的键，提高性能
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(font));
            }
        }

        // 确保重要的字体属性被设置
        for (String key : FONT_UI_KEYS) {
            UIManager.put(key, new FontUIResource(font));
        }
    }

    /**
     * 获取指定样式和大小的字体，优先使用给定的字体列表中的字体。
     *
     * @param style     字体样式
     * @param size      字体大小
     * @param fontNames 字体名称列表
     * @return Font 如果找到可用字体则返回，否则返回 null
     */
    private static Font findAvailableFont(int style, int size, String[] fontNames) {
        if (fontNames == null) {
            return null;
        }

        for (String fontName : fontNames) {
            if (fontName == null || fontName.trim().isEmpty()) {
                continue;
            }

            Font font = new Font(fontName, style, size);
            // 改进字体检查逻辑：检查字体族名是否匹配或能显示常用中文字符
            if (isFontAvailable(font, fontName)) {
                return font;
            }
        }
        return null;
    }

    /**
     * 检查字体是否可用
     *
     * @param font     待检查的字体
     * @param fontName 原始字体名称
     * @return true 如果字体可用
     */
    private static boolean isFontAvailable(Font font, String fontName) {
        // 检查字体族名是否匹配（忽略大小写）
        if (font.getFamily().equalsIgnoreCase(fontName) ||
                font.getFontName().equalsIgnoreCase(fontName)) {
            return true;
        }

        // 检查是否能显示常用的中文字符
        String testChars = "测试中文字体显示";
        for (int i = 0; i < testChars.length(); i++) {
            if (!font.canDisplay(testChars.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
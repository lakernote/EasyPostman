package com.laker.postman.util;

import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

/**
 * 字体管理工具类
 * 负责应用全局字体设置
 */
@Slf4j
@UtilityClass
public class FontManager {

    /**
     * 应用保存的字体设置到整个应用
     */
    public static void applyFontSettings() {
        String fontName = SettingManager.getUiFontName();
        int fontSize = SettingManager.getUiFontSize();

        applyFont(fontName, fontSize);
    }

    /**
     * 应用指定的字体到整个应用
     *
     * @param fontName 字体名称，空字符串表示使用系统默认
     * @param fontSize 字体大小
     */
    public static void applyFont(String fontName, int fontSize) {
        try {
            // 如果字体名称为空，使用系统默认字体
            if (fontName == null || fontName.isEmpty()) {
                fontName = getSystemDefaultFontName();
            }

            log.info("Applying font: {} with size: {}", fontName, fontSize);

            // 创建字体资源
            Font plainFont = new Font(fontName, Font.PLAIN, fontSize);
            Font boldFont = new Font(fontName, Font.BOLD, fontSize);
            Font italicFont = new Font(fontName, Font.ITALIC, fontSize);

            // 更新 UIManager 中的所有字体
            UIDefaults defaults = UIManager.getDefaults();
            Enumeration<Object> keys = defaults.keys();

            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = defaults.get(key);

                if (value instanceof FontUIResource originalFont) {
                    // 根据原始字体的样式选择对应的新字体
                    Font newFont;
                    int style = originalFont.getStyle();
                    if (style == Font.BOLD) {
                        newFont = boldFont;
                    } else if (style == Font.ITALIC) {
                        newFont = italicFont;
                    } else if (style == (Font.BOLD | Font.ITALIC)) {
                        newFont = new Font(fontName, Font.BOLD | Font.ITALIC, fontSize);
                    } else {
                        newFont = plainFont;
                    }

                    defaults.put(key, new FontUIResource(newFont));
                }
            }

            // 更新所有已存在的窗口
            updateExistingWindows();

            log.info("Font applied successfully");
        } catch (Exception e) {
            log.error("Failed to apply font settings", e);
        }
    }

    /**
     * 获取系统默认字体名称
     */
    private static String getSystemDefaultFontName() {
        Font defaultFont = UIManager.getFont("Label.font");
        if (defaultFont != null) {
            return defaultFont.getName();
        }

        // 如果无法获取，返回一个通用的默认值
        return Font.SANS_SERIF;
    }

    /**
     * 更新所有已存在的窗口
     */
    private static void updateExistingWindows() {
        try {
            Window[] windows = Window.getWindows();
            log.info("Updating {} window(s)", windows.length);

            for (Window window : windows) {
                if (window.isDisplayable()) {
                    // 先更新组件树 UI
                    SwingUtilities.updateComponentTreeUI(window);

                    // 强制重新验证和重绘
                    window.validate();
                    window.repaint();

                    log.debug("Updated window: {}", window.getClass().getSimpleName());
                }
            }

            log.info("All windows updated successfully");
        } catch (Exception e) {
            log.error("Failed to update windows", e);
        }
    }

    /**
     * 递归更新组件及其子组件的字体
     *
     * @param component 要更新的组件
     * @param font      新字体
     */
    public static void updateComponentFont(Component component, Font font) {
        if (component == null || font == null) {
            return;
        }

        component.setFont(font);

        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                updateComponentFont(child, font);
            }
        }
    }
}


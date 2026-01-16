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
 * <p>
 * 重要说明 - 关于 emoji 显示问题：
 * 在所有操作系统下，emoji 的显示都依赖于字体降级链（font fallback chain）。
 * 当主字体无法显示 emoji 字符时，系统会自动降级到相应的 emoji 字体：
 * <ul>
 *   <li>Windows: Segoe UI Emoji</li>
 *   <li>macOS: Apple Color Emoji</li>
 *   <li>Linux: Noto Color Emoji</li>
 * </ul>
 * <p>
 * 使用 new Font() 直接创建字体会丢失降级链，导致 emoji 无法显示。
 * 必须使用 deriveFont() 方法从系统字体派生，以保留字体降级链。
 */
@Slf4j
@UtilityClass
public class FontManager {

    private static final String LABEL_FONT_KEY = "Label.font";

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
            log.info("Applying font: {} with size: {}", fontName, fontSize);

            // 更新 UIManager 中的所有字体
            UIDefaults defaults = UIManager.getDefaults();
            Enumeration<Object> keys = defaults.keys();

            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = defaults.get(key);

                if (value instanceof FontUIResource originalFont) {
                    // 使用 deriveFont 保留字体降级链，支持 emoji 等特殊字符
                    Font newFont = createFontWithFallback(fontName, originalFont.getStyle(), fontSize);
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
     * 创建字体，保留字体降级链以支持 emoji 等特殊字符
     * <p>
     * 关键实现策略：
     * <ol>
     *   <li>优先使用 deriveFont() 从系统字体派生，保留字体降级链</li>
     *   <li>只有当用户指定了不同的自定义字体时，才使用 new Font()</li>
     *   <li>使用 new Font() 会丢失降级链，导致 emoji 显示问题（会记录警告日志）</li>
     * </ol>
     *
     * @param fontName 字体名称，空字符串表示使用系统默认
     * @param style    字体样式（Font.PLAIN, Font.BOLD, Font.ITALIC 或其组合）
     * @param size     字体大小
     * @return 创建的字体
     */
    @SuppressWarnings("MagicConstant") // style 参数是 int 类型，但实际值是 Font 的样式常量
    private static Font createFontWithFallback(String fontName, int style, float size) {
        // 步骤1: 获取系统默认字体作为基础
        Font baseFont = UIManager.getFont(LABEL_FONT_KEY);
        if (baseFont == null) {
            baseFont = new JLabel().getFont();
        }

        // 步骤2: 如果未指定字体或指定的是系统默认字体，使用 deriveFont 保留降级链
        if (fontName == null || fontName.isEmpty() ||
                baseFont.getName().equals(fontName) || baseFont.getFamily().equals(fontName)) {
            // ✅ 正确做法：使用 deriveFont 保留字体降级链，支持 emoji
            return baseFont.deriveFont(style, size);
        }

        // 步骤3: 用户指定了自定义字体，创建新字体实例
        // ⚠️ 警告：直接 new Font() 会丢失字体降级链
        // 这会导致 emoji 在所有平台（Windows/macOS/Linux）下都可能显示为方框
        // 建议用户使用系统默认字体以获得最佳的 emoji 和多语言字符支持
        log.warn("Creating font '{}' will lose font fallback chain, emoji and some Unicode characters may not display correctly",
                 fontName);
        return new Font(fontName, style, (int) size);
    }

    /**
     * 更新所有已存在的窗口
     */
    private static void updateExistingWindows() {
        log.info("Updating all windows for font change...");

        // 使用统一的刷新管理器更新所有窗口
        UIRefreshManager.refreshAllWindows();

        log.info("All windows updated successfully for font change");
    }

    /**
     * 递归更新组件及其子组件的字体
     * <p>
     * ⚠️ 注意事项：
     * <ul>
     *   <li>此方法会直接覆盖组件的字体，可能丢失字体降级链</li>
     *   <li>建议优先使用 {@link #applyFont(String, int)} 来全局更新字体</li>
     *   <li>此方法适用于需要单独控制特定组件字体的场景</li>
     * </ul>
     *
     * @param component 要更新的组件
     * @param font      新字体（建议使用 deriveFont 创建以保留降级链）
     */
    public static void updateComponentFont(Component component, Font font) {
        if (component == null || font == null) {
            return;
        }

        component.setFont(font);

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                updateComponentFont(child, font);
            }
        }
    }
}


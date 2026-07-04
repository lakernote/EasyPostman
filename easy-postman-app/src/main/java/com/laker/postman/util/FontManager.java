package com.laker.postman.util;

import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Locale;

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

    private static final String DEFAULT_FONT_KEY = "defaultFont";
    private static final String LABEL_FONT_KEY = "Label.font";
    private static volatile Font lookAndFeelDefaultFont;

    /**
     * 应用保存的字体设置到整个应用
     */
    public static void applyFontSettings() {
        try {
            installSavedFontDefaults();
            updateExistingWindows();
            log.info("Font applied successfully");
        } catch (Exception e) {
            log.error("Failed to apply font settings", e);
        }
    }

    /**
     * 应用指定的字体到整个应用
     *
     * @param fontName 字体名称，空字符串表示使用系统默认
     * @param fontSize 字体大小
     */
    public static void applyFont(String fontName, int fontSize) {
        try {
            installFontDefaults(fontName, fontSize);

            // 更新所有已存在的窗口
            updateExistingWindows();

            log.info("Font applied successfully");
        } catch (Exception e) {
            log.error("Failed to apply font settings", e);
        }
    }

    /**
     * 将已保存的字体设置安装到 FlatLaf defaultFont，不立即刷新窗口。
     * 主题切换会重建 UI defaults，因此切换 Look and Feel 后需要先重装字体，再统一刷新。
     */
    public static Font installSavedFontDefaults() {
        String fontName = resolveAllowedSavedUiFontName(SettingManager.getUiFontName(), I18nUtil.currentLocale());
        return installFontDefaults(fontName, SettingManager.getUiFontSize());
    }

    /**
     * 捕获当前 Look and Feel 安装后的原始 UI 字体。
     * <p>
     * FlatLaf 切换主题会重建 UIDefaults。用户切换到自定义字体后，Label.font/defaultFont
     * 可能已经被自定义字体污染；再次选择“系统默认”时不能继续从当前 Label.font 派生，
     * 否则会把不支持中文的物理字体继续保留下来。
     */
    public static void captureLookAndFeelDefaultFont() {
        lookAndFeelDefaultFont = resolveCurrentLookAndFeelFont();
    }

    /**
     * 安装 FlatLaf 推荐的全局 defaultFont，不立即刷新窗口。
     *
     * @param fontName 字体名称，空字符串表示使用系统默认
     * @param fontSize 字体大小
     * @return 已安装的字体
     */
    public static Font installFontDefaults(String fontName, int fontSize) {
        log.info("Installing font defaults: {} with size: {}", fontName, fontSize);
        Font newFont = createFontWithFallback(fontName, Font.PLAIN, fontSize);
        UIManager.put(DEFAULT_FONT_KEY, new FontUIResource(newFont));
        return newFont;
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
        Font systemDefaultFont = resolveSystemDefaultFont();

        // 步骤2: 如果未指定字体或指定的是系统默认字体，使用 deriveFont 保留降级链
        if (fontName == null || fontName.isEmpty() ||
                systemDefaultFont.getName().equals(fontName) || systemDefaultFont.getFamily().equals(fontName)) {
            // ✅ 正确做法：使用 deriveFont 保留字体降级链，支持 emoji
            return systemDefaultFont.deriveFont(style, size);
        }

        // 步骤3: 用户指定了自定义字体，创建新字体实例
        // ⚠️ 警告：直接 new Font() 会丢失字体降级链
        // 这会导致 emoji 在所有平台（Windows/macOS/Linux）下都可能显示为方框
        // 建议用户使用系统默认字体以获得最佳的 emoji 和多语言字符支持
        log.warn("Creating font '{}' will lose font fallback chain, emoji and some Unicode characters may not display correctly",
                 fontName);
        return new Font(fontName, style, (int) size);
    }

    private static Font resolveSystemDefaultFont() {
        Font capturedFont = lookAndFeelDefaultFont;
        if (capturedFont != null) {
            return capturedFont;
        }

        Font currentLookAndFeelFont = resolveCurrentLookAndFeelFont();
        lookAndFeelDefaultFont = currentLookAndFeelFont;
        return currentLookAndFeelFont;
    }

    public static String resolveAllowedUiFontNameForLocale(String fontName, Locale locale) {
        if (fontName == null || fontName.isBlank()) {
            return "";
        }
        UiFontCatalog.FontSupport support = UiFontCatalog.inspectFamily(fontName);
        return resolveAllowedUiFontName(fontName, locale, support);
    }

    private static String resolveAllowedSavedUiFontName(String fontName, Locale locale) {
        return resolveAllowedUiFontNameForLocale(fontName, locale);
    }

    static String resolveAllowedUiFontName(String fontName, Locale locale, UiFontCatalog.FontSupport support) {
        if (fontName == null || fontName.isBlank()) {
            return "";
        }
        if (UiFontCatalog.isUiFontAllowedForLocale(support, locale)) {
            return fontName;
        }
        log.warn("Ignoring saved UI font '{}' because it is unsafe for locale {}", fontName, locale);
        return "";
    }

    private static Font resolveCurrentLookAndFeelFont() {
        Font baseFont = UIManager.getFont(LABEL_FONT_KEY);
        if (baseFont == null) {
            baseFont = UIManager.getFont(DEFAULT_FONT_KEY);
        }
        if (baseFont == null) {
            baseFont = new JLabel().getFont();
        }
        return baseFont == null ? new Font(Font.DIALOG, Font.PLAIN, SettingManager.getUiFontSize()) : baseFont;
    }

    /**
     * 更新所有已存在的窗口
     */
    private static void updateExistingWindows() {
        // 使用统一的刷新管理器更新所有窗口
        UIRefreshManager.refreshAllWindows();
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

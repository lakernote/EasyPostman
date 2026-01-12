package com.laker.postman.common.themes;

import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.UserSettingsUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 简单主题管理器
 * 支持亮色和暗色两种主题切换
 */
@Slf4j
@UtilityClass
public class SimpleThemeManager {

    private static final String THEME_SETTING_KEY = "ui.theme";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    private static String currentTheme = THEME_LIGHT;

    /**
     * 初始化主题（应用启动时调用）
     */
    public static void initTheme() {
        // 从 UserSettings 中读取上次选择的主题
        String savedTheme = UserSettingsUtil.getString(THEME_SETTING_KEY);
        currentTheme = (savedTheme != null) ? savedTheme : THEME_LIGHT;
        applyTheme(currentTheme, false);
    }

    /**
     * 切换到亮色主题
     */
    public static void switchToLightTheme() {
        if (!THEME_LIGHT.equals(currentTheme)) {
            applyTheme(THEME_LIGHT, true);
        }
    }

    /**
     * 切换到暗色主题
     */
    public static void switchToDarkTheme() {
        if (!THEME_DARK.equals(currentTheme)) {
            applyTheme(THEME_DARK, true);
        }
    }

    /**
     * 获取当前主题
     */
    public static boolean isLightTheme() {
        return THEME_LIGHT.equals(currentTheme);
    }

    /**
     * 获取当前主题
     */
    public static boolean isDarkTheme() {
        return THEME_DARK.equals(currentTheme);
    }

    /**
     * 应用主题
     *
     * @param theme            主题名称
     * @param showNotification 是否显示通知
     */
    private static void applyTheme(String theme, boolean showNotification) {
        try {
            boolean success;
            if (THEME_DARK.equals(theme)) {
                success = EasyDarkLaf.setup();
            } else {
                success = EasyLightLaf.setup();
            }

            if (success) {
                currentTheme = theme;

                // 保存主题设置到 UserSettings
                UserSettingsUtil.set(THEME_SETTING_KEY, theme);

                // 更新所有已打开的窗口
                updateAllWindows();

                log.info("Applied theme: {}", theme);

                if (showNotification) {
                    String message = THEME_DARK.equals(theme)
                            ? I18nUtil.getMessage(MessageKeys.THEME_SWITCHED_TO_DARK)
                            : I18nUtil.getMessage(MessageKeys.THEME_SWITCHED_TO_LIGHT);
                    NotificationUtil.showSuccess(message);
                }
            }
        } catch (Exception e) {
            log.error("Failed to apply theme: {}", theme, e);
            if (showNotification) {
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE, e.getMessage()));
            }
        }
    }

    /**
     * 更新所有已打开的窗口
     */
    private static void updateAllWindows() {
        for (Window window : Window.getWindows()) {
            // 在 macOS 上更新窗口外观模式以匹配主题
            // 这对于透明标题栏的窗口特别重要，可以确保标题栏文字颜色正确显示
            if (SystemInfo.isMacOS && window instanceof JFrame frame) {
                JRootPane rootPane = frame.getRootPane();

                // 设置 macOS 窗口外观模式：dark 或 light
                // NSAppearanceNameVibrantDark: 暗色外观，标题栏文字为白色
                // NSAppearanceNameVibrantLight: 亮色外观，标题栏文字为黑色
                if (THEME_DARK.equals(currentTheme)) {
                    rootPane.putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantDark");
                } else {
                    rootPane.putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantLight");
                }
            }

            // 更新窗口组件树的 UI，应用新主题样式
            SwingUtilities.updateComponentTreeUI(window);
        }
    }
}

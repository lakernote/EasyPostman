package com.laker.postman.common.themes;

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
            SwingUtilities.updateComponentTreeUI(window);
        }
    }
}

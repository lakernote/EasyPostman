package com.laker.postman.common.themes;

import com.laker.postman.common.component.notification.NotificationCenter;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 简单主题管理器
 * 支持亮色和暗色两种主题切换
 */
@Slf4j
@UtilityClass
public class SimpleThemeManager {

    private static final String THEME_SETTING_KEY = "ui.theme";

    private static String currentThemeId = ThemeRegistry.DEFAULT_THEME_ID;
    private static final Set<String> registeredDefaultsSources = new HashSet<>();

    /**
     * 初始化主题（应用启动时调用）
     */
    public static void initTheme() {
        // 从 UserSettings 中读取上次选择的主题
        String savedTheme = UserPreferencesStore.getString(THEME_SETTING_KEY);
        ThemeDescriptor theme = ThemeRegistry.getOrDefault(savedTheme);
        currentThemeId = theme.id();
        applyTheme(theme, false);
    }

    public static void switchTheme(String themeId) {
        ThemeRegistry.find(themeId).ifPresent(theme -> {
            if (!theme.id().equals(currentThemeId)) {
                switchWithAnimation(() -> applyTheme(theme, true));
            }
        });
    }

    public static List<ThemeDescriptor> availableThemes() {
        return ThemeRegistry.all();
    }

    public static String currentThemeId() {
        return currentThemeId;
    }

    private static ThemeDescriptor currentTheme() {
        return ThemeRegistry.getOrDefault(currentThemeId);
    }

    private static void applyEditorThemeResources(ThemeDescriptor theme) {
        EditorThemeUtil.configureEditorFontApplier(EditorFontManager::applyConfiguredEditorFont);
        if (theme != null) {
            EditorThemeUtil.configureThemeResources(
                    theme.editorThemeResourcePath(),
                    theme.fallbackEditorThemeResourcePath()
            );
        }
    }

    /**
     * 使用动画过渡执行主题切换。
     * 先截取当前界面快照，再通过 invokeLater 在下一帧执行切换，
     * 保证快照完整渲染后动画才开始，避免 EDT 阻塞导致动画失效。
     */
    private static void switchWithAnimation(Runnable themeSwitch) {
        FlatAnimatedLafChange.showSnapshot();
        SwingUtilities.invokeLater(() -> {
            themeSwitch.run();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        });
    }

    /**
     * 获取当前主题
     */
    public static boolean isDarkTheme() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf instanceof FlatLaf) {
            return FlatLaf.isLafDark();
        }
        return currentTheme().dark();
    }

    /**
     * 应用主题
     *
     * @param theme            主题名称
     * @param showNotification 是否显示通知
     */
    private static void applyTheme(ThemeDescriptor theme, boolean showNotification) {
        try {
            ensureDefaultsSourceRegistered(theme);

            boolean success = theme.applyLookAndFeel();
            if (success) {
                currentThemeId = theme.id();
                applyEditorThemeResources(theme);

                // Look and Feel 切换会重建 UIDefaults，先恢复用户字体再刷新窗口。
                FontManager.captureLookAndFeelDefaultFont();
                FontManager.installSavedFontDefaults();
                ToolWindowSurfaceStyle.installGlobalDialogWindowChrome();

                UserPreferencesStore.put(THEME_SETTING_KEY, theme.id());

                // 更新所有已打开的窗口
                updateAllWindows();

                log.info("Applied theme: {}", theme.id());

                if (showNotification) {
                    NotificationCenter.showSuccess(switchNotificationMessage(theme));
                }
            }
        } catch (Exception e) {
            log.error("Failed to apply theme: {}", theme.id(), e);
            if (showNotification) {
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE, e.getMessage()));
            }
        }
    }

    private static void ensureDefaultsSourceRegistered(ThemeDescriptor theme) {
        String defaultsSource = theme.flatLafDefaultsSource();
        if (defaultsSource == null || defaultsSource.isBlank() || registeredDefaultsSources.contains(defaultsSource)) {
            return;
        }
        FlatLaf.registerCustomDefaultsSource(defaultsSource);
        registeredDefaultsSources.add(defaultsSource);
    }

    private static String switchNotificationMessage(ThemeDescriptor theme) {
        if (theme.dark()) {
            return I18nUtil.getMessage(MessageKeys.THEME_SWITCHED_TO_DARK);
        }
        return I18nUtil.getMessage(MessageKeys.THEME_SWITCHED_TO_LIGHT);
    }

    /**
     * 更新所有已打开的窗口
     */
    private static void updateAllWindows() {
        // macOS 特殊处理：更新窗口外观模式
        if (SystemInfo.isMacOS) {
            for (Window window : Window.getWindows()) {
                if (window instanceof JFrame frame) {
                    JRootPane rootPane = frame.getRootPane();

                    // 设置 macOS 窗口外观模式：dark 或 light
                    // NSAppearanceNameVibrantDark: 暗色外观，标题栏文字为白色
                    // NSAppearanceNameVibrantLight: 亮色外观，标题栏文字为黑色
                    if (isDarkTheme()) {
                        rootPane.putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantDark");
                    } else {
                        rootPane.putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantLight");
                    }
                }
            }
        }

        // 使用统一的刷新管理器更新所有窗口
        UIRefreshManager.refreshAllWindows();
    }
}

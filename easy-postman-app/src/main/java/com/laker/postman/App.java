package com.laker.postman;

import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.common.window.SplashWindow;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.startup.StartupCoordinator;
import com.laker.postman.util.*;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 程序入口类。
 */
@Slf4j
public class App {
    public static void main(String[] args) {
        // 0. 设置全局异常处理器
        setupGlobalExceptionHandler();

        // 1. 配置平台特定的窗口装饰
        configurePlatformSpecificSettings();

        // 2. 在事件分派线程（EDT）中初始化 UI
        SwingUtilities.invokeLater(() -> {
            initializeLookAndFeel();
            initializeUI();
        });

        // 3. 注册应用程序关闭钩子
        registerShutdownHook();
    }

    /**
     * 配置平台特定的设置。
     * <p>
     * Linux 平台：启用自定义窗口装饰以获得更好的原生外观体验。
     * 参考：<a href="https://www.formdev.com/flatlaf/window-decorations/">flatlaf 文档</a>
     * </p>
     */
    private static void configurePlatformSpecificSettings() {
        if (SystemInfo.isLinux) {
            log.debug("Detected Linux platform, enabling custom window decorations");
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }
    }

    /**
     * 初始化外观主题。
     * <p>
     * 样式配置文件：com/laker/postman/common/themes/EasyLightLaf.properties
     */
    private static void initializeLookAndFeel() {
        SimpleThemeManager.initTheme();
        FontManager.applyFontSettings();
    }

    /**
     * 初始化用户界面。
     */
    private static void initializeUI() {
        IconFontSwing.register(FontAwesome.getIconFont());
        StartupCoordinator startupCoordinator = new StartupCoordinator();

        if (!isSplashEnabled()) {
            initializeMainFrameWithoutSplash(startupCoordinator);
            return;
        }

        SplashWindow splash = new SplashWindow();
        splash.init();
        splash.initMainFrame(startupCoordinator);
    }

    private static void initializeMainFrameWithoutSplash(StartupCoordinator startupCoordinator) {
        SwingWorker<MainFrame, Void> worker = new SwingWorker<>() {
            @Override
            protected MainFrame doInBackground() {
                try {
                    return startupCoordinator.prepareMainFrame(null);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to prepare main frame", e);
                }
            }

            @Override
            protected void done() {
                try {
                    MainFrame mainFrame = get();
                    startupCoordinator.showMainFrame(mainFrame);
                    startupCoordinator.scheduleBackgroundUpdateCheck();
                } catch (Exception e) {
                    log.error("Failed to initialize main frame without splash", e);
                    JOptionPane.showMessageDialog(
                            null,
                            I18nUtil.getMessage(MessageKeys.SPLASH_ERROR_LOAD_MAIN),
                            I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                            JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(1);
                }
            }
        };
        worker.execute();
    }

    private static boolean isSplashEnabled() {
        return SettingManager.isStartupSplashEnabled();
    }

    /**
     * 设置全局未捕获异常处理器。
     */
    private static void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // 过滤掉应该被忽略的异常
            if (ExceptionUtil.shouldIgnoreException(throwable)) {
                log.debug("Ignoring harmless exception in thread: {}", thread.getName(), throwable);
                return;
            }

            // 记录错误并通知用户
            log.error("Uncaught exception in thread: {}", thread.getName(), throwable);
            SwingUtilities.invokeLater(App::notifyUnhandledException);
        });
    }

    private static void notifyUnhandledException() {
        String message = I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE);
        if (isMainFrameVisible()) {
            NotificationUtil.showError(message);
            return;
        }

        JOptionPane.showMessageDialog(
                null,
                message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static boolean isMainFrameVisible() {
        for (Window window : Window.getWindows()) {
            if (window instanceof MainFrame && window.isShowing()) {
                return true;
            }
        }
        return false;
    }


    /**
     * 注册应用程序关闭钩子
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                PluginRuntime.shutdown();
                BeanFactory.destroy();
                log.info("Application shutdown completed");
            } catch (Exception e) {
                log.error("Error during application shutdown", e);
            }
        }, "ShutdownHook"));
    }
}

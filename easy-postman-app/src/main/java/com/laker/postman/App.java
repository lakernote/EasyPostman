package com.laker.postman;

import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.AppConstants;
import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.common.window.SplashWindow;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.service.UpdateService;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.ExceptionUtil;
import com.laker.postman.util.FontManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * 程序入口类。
 */
@Slf4j
public class App {
    private static final int UPDATE_CHECK_DELAY_MS = 2000;


    public static void main(String[] args) {
        // 1. 配置平台特定的窗口装饰
        configurePlatformSpecificSettings();

        // 2. 在事件分派线程（EDT）中初始化 UI
        SwingUtilities.invokeLater(() -> {
            initializeLookAndFeel();
            initializeUI();
        });

        // 3. 设置全局异常处理器
        setupGlobalExceptionHandler();

        // 4. 注册应用程序关闭钩子
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

        if (!isSplashEnabled()) {
            initializeMainFrameWithoutSplash();
            return;
        }

        SplashWindow splash = new SplashWindow();
        splash.init();
        splash.initMainFrame();
    }

    private static void initializeMainFrameWithoutSplash() {
        SwingWorker<MainFrame, Void> worker = new SwingWorker<>() {
            @Override
            protected MainFrame doInBackground() {
                BeanFactory.init(AppConstants.BASE_PACKAGE);

                PluginRuntime.initialize();

                MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
                mainFrame.initComponents();
                return mainFrame;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    try {
                        MainFrame mainFrame = get();
                        mainFrame.setVisible(true);
                        mainFrame.toFront();
                        mainFrame.requestFocus();
                        scheduleBackgroundUpdateCheck();
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
                });
            }
        };
        worker.execute();
    }

    private static void scheduleBackgroundUpdateCheck() {
        Timer delayTimer = new Timer(UPDATE_CHECK_DELAY_MS, e -> {
            try {
                BeanFactory.getBean(UpdateService.class).checkUpdateOnStartup();
                log.debug("Background update check scheduled after main window became visible");
            } catch (Exception ex) {
                log.warn("Failed to start background update check", ex);
            }
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
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
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE
            ));
        });
    }


    /**
     * 注册应用程序关闭钩子
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 销毁 IOC 容器，调用所有 @PreDestroy 方法
                PluginRuntime.shutdown();
                BeanFactory.destroy();
                log.info("Application shutdown completed");
            } catch (Exception e) {
                log.error("Error during application shutdown", e);
            }
        }, "ShutdownHook"));
    }
}

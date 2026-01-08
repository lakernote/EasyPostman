package com.laker.postman;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.window.SplashWindow;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.service.UpdateService;
import com.laker.postman.util.*;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * 程序入口类。
 */
@Slf4j
public class App {


    public static void main(String[] args) {
        // 0. 初始化 IOC 容器（在 EDT 之前，避免阻塞 UI）
        BeanFactory.init("com.laker.postman");

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

        // 5. 启动后台版本检查
        BeanFactory.getBean(UpdateService.class).checkUpdateOnStartup();
    }

    /**
     * 配置平台特定的设置。
     * <p>
     * Linux 平台：启用自定义窗口装饰以获得更好的原生外观体验。
     * 参考：https://www.formdev.com/flatlaf/window-decorations/
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
     * 配置加载顺序：
     * 1. FlatLaf.properties - FlatLaf 基础配置
     * 2. FlatLightLaf.properties - 浅色主题配置
     * 3. themes/FlatIntelliJLaf.properties - 自定义配置（会自动加载）
     * <p>
     * 所有 UI 样式配置都在 themes/FlatIntelliJLaf.properties 中定义。
     */
    private static void initializeLookAndFeel() {
        // 注册自定义主题目录
        FlatLaf.registerCustomDefaultsSource("themes");
        // 加载 FlatLaf 主题（会自动加载 themes/FlatIntelliJLaf.properties）
        FlatIntelliJLaf.setup();
        // 应用用户保存的字体设置
        FontManager.applyFontSettings();
    }

    /**
     * 初始化用户界面。
     */
    private static void initializeUI() {
        // 注册图标字体
        IconFontSwing.register(FontAwesome.getIconFont());

        // 显示启动画面并异步加载主窗口
        SplashWindow splash = BeanFactory.getBean(SplashWindow.class);
        splash.initMainFrame();
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
            log.info("Application shutting down...");
            try {
                // 销毁 IOC 容器，调用所有 @PreDestroy 方法
                BeanFactory.destroy();
                log.info("Application shutdown completed");
            } catch (Exception e) {
                log.error("Error during application shutdown", e);
            }
        }, "ShutdownHook"));
    }
}
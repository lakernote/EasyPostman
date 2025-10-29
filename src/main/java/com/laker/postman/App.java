package com.laker.postman;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.laker.postman.common.window.SplashWindow;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.service.UpdateService;
import com.laker.postman.util.StyleUtils;
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


    public static void main(String[] args) {
        // 0. 初始化 IOC 容器（在 EDT 之前，避免阻塞 UI）
        // 扫描 com.laker.postman 包下的所有 @Component 注解的类
        BeanFactory.init("com.laker.postman");

        // Swing 推荐在事件分派线程（EDT）中运行所有 UI 相关操作
        SwingUtilities.invokeLater(() -> {
            // 1. 设置主题
            FlatIntelliJLaf.setup();
            // 2. FlatLaf 统一商务风格属性（圆角、阴影等）
            StyleUtils.apply();
            // 3. 注册图标字体，使用 FontAwesome 图标库
            IconFontSwing.register(FontAwesome.getIconFont());
            // 4. 从 IOC 容器获取 SplashWindow
            SplashWindow splash = BeanFactory.getBean(SplashWindow.class);
            // 5. 异步加载主窗口
            splash.initMainFrame();
        });

        // 6. 设置全局异常处理器，防止程序因未捕获异常崩溃
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception in thread: {}", thread.getName(), throwable);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE
            ));
        });

        // 7. 注册应用程序关闭钩子，确保优雅关闭
        registerShutdownHook();

        // 8. 启动后台版本检查
        UpdateService.getInstance().checkUpdateOnStartup();
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
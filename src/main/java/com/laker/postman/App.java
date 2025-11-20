package com.laker.postman;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.laker.postman.common.window.SplashWindow;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.service.UpdateService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.StyleUtils;
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
            // 过滤掉SSH executor shutdown的错误 - 这是正常的cleanup过程
            // 这些错误发生在Git操作完成后，SSH连接的异步IO操作尝试使用已关闭的executor
            // 这是JGit + Apache SSHD的已知行为，不影响功能
            if (isHarmlessSshShutdownError(throwable)) {
                log.debug("Ignoring harmless SSH executor shutdown error in thread: {}", thread.getName());
                return;
            }

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
        BeanFactory.getBean(UpdateService.class).checkUpdateOnStartup();
    }

    /**
     * 检查是否是无害的SSH executor shutdown错误
     * 这些错误通常发生在Git SSH操作完成后，异步IO操作尝试使用已关闭的executor
     * 这是Apache SSHD + JGit的已知行为，不影响实际功能
     */
    private static boolean isHarmlessSshShutdownError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        // 检查是否是SSH executor shutdown错误
        if (throwable instanceof IllegalStateException) {
            String message = throwable.getMessage();
            if (message != null && message.contains("Executor has been shut down")) {
                // 检查调用栈是否包含SSH相关的类
                for (StackTraceElement element : throwable.getStackTrace()) {
                    String className = element.getClassName();
                    if (className.contains("org.apache.sshd") ||
                        className.contains("NoCloseExecutor") ||
                        className.contains("AsynchronousChannelGroup") ||
                        className.contains("AsynchronousSocketChannel")) {
                        return true;
                    }
                }
            }
        }

        return false;
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
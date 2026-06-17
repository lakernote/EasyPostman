package com.laker.postman.startup;

import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.http.runtime.app.AppHttpRuntimeBootstrap;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.OptionalInt;

/**
 * 应用启动器：负责主入口之后的启动编排。
 */
@Slf4j
@UtilityClass
public class AppLauncher {
    public static final int GUI_STARTED = Integer.MIN_VALUE;

    public int launch(String[] args) {
        configureBaseRuntimeEnvironment();
        registerShutdownHook();
        OptionalInt commandExitCode = new AppCommandRouter().route(args, System.out, System.err);
        if (commandExitCode.isPresent()) {
            return commandExitCode.getAsInt();
        }
        // 先完成 Swing 平台配置，再把组件初始化切到 EDT，避免 UI 线程外创建组件。
        configurePlatformWindowDecorations();
        SwingUtilities.invokeLater(AppLauncher::startSwingApplication);
        return GUI_STARTED;
    }

    private void configureBaseRuntimeEnvironment() {
        // 这些配置对 GUI 和 headless CLI 都有效，且不应触发 Swing 初始化。
        Thread.setDefaultUncaughtExceptionHandler(new AppUncaughtExceptionHandler());
        System.setProperty("java.net.useSystemProxies", "true");
        AppHttpRuntimeBootstrap.configure();
    }

    private void configurePlatformWindowDecorations() {
        if (!SystemInfo.isLinux) {
            return;
        }
        log.debug("Detected Linux platform, enabling custom window decorations");
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
    }

    private void startSwingApplication() {
        // Swing 组件创建前先确定主题；主题初始化会同步恢复用户字体 defaults。
        initializeLookAndFeel();
        startMainFrame();
    }

    /**
     * Look and feel 必须在 Swing 组件创建前完成，否则首屏组件会混用旧 UI defaults。
     */
    private void initializeLookAndFeel() {
        SimpleThemeManager.initTheme();
    }

    private void startMainFrame() {
        StartupCoordinator startupCoordinator = new StartupCoordinator();
        if (SettingManager.isStartupSplashEnabled()) {
            // Splash 模式下先显示轻量过渡窗口，主窗口 shell 准备完成后再切换。
            startWithSplash(startupCoordinator);
            return;
        }
        // 无 Splash 模式仍使用后台线程准备主窗口，避免阻塞 EDT。
        new NoSplashStartupWorker(startupCoordinator).execute();
    }

    private void startWithSplash(StartupCoordinator startupCoordinator) {
        SplashWindow splash = new SplashWindow();
        splash.init();
        splash.startMainFrameInitialization(startupCoordinator);
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 退出时先关闭插件运行时，再销毁宿主 IOC，避免插件回调访问已释放的 Bean。
                PluginRuntime.shutdown();
                BeanFactory.destroy();
                log.info("Application shutdown completed");
            } catch (Exception e) {
                log.error("Error during application shutdown", e);
            }
        }, "ShutdownHook"));
    }
}

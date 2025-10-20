package com.laker.postman;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.laker.postman.common.window.EasyPostManSplashWindow;
import com.laker.postman.service.UpdateService;
import com.laker.postman.util.EasyPostManStyleUtils;
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
        // Swing 推荐在事件分派线程（EDT）中运行所有 UI 相关操作
        SwingUtilities.invokeLater(() -> {
            // 1. 设置主题
            FlatIntelliJLaf.setup();
            // 2. FlatLaf 统一商务风格属性（圆角、阴影等）
            EasyPostManStyleUtils.apply();
            // 3. 注册图标字体，使用 FontAwesome 图标库
            IconFontSwing.register(FontAwesome.getIconFont());
            // 4. 显示 SplashWindow
            EasyPostManSplashWindow splash = new EasyPostManSplashWindow();
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

        // 7. 启动后台版本检查
        UpdateService.getInstance().checkUpdateOnStartup();
    }
}
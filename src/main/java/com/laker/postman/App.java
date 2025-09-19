package com.laker.postman;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.laker.postman.common.EasyPostManSplashWindow;
import com.laker.postman.service.UpdateService;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.EasyPostManStyleUtils;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
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
        // Swing 推荐在事件分派线程（EDT）中运行所有 UI 相关操作
        SwingUtilities.invokeLater(() -> {
            // 1. 设置全局字体，适配不同操作系统，提升商务观感
            Font font = EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11);
            // 设置全局字体（包括菜单、弹窗等）
            EasyPostManFontUtil.setupGlobalFont(font);
            // 2. 设置主题
            FlatIntelliJLaf.setup();
            // 3. FlatLaf 统一商务风格属性（圆角、阴影等）
            EasyPostManStyleUtils.apply();
            // 4. 注册图标字体，使用 FontAwesome 图标库
            IconFontSwing.register(FontAwesome.getIconFont());
            // 5. 显示 SplashWindow
            EasyPostManSplashWindow splash = new EasyPostManSplashWindow();
            // 6. 异步加载主窗口
            splash.initMainFrame();
            // 7. 启动后台版本检查
            UpdateService.getInstance().checkUpdateOnStartup();
        });

        // 8. 设置全局异常处理器，防止程序因未捕获异常崩溃
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception in thread: {}", thread.getName(), throwable);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE
            ));
        });
    }
}
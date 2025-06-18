package com.laker.postman;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.util.BusinessStyleUtils;
import com.laker.postman.util.FontUtil;
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
            Font font = FontUtil.getDefaultFont(Font.PLAIN, 12);
            // 设置全局字体（包括菜单、弹窗等）
            FontUtil.setupGlobalFont(font);

            // 2. 设置主题
            try {
                FlatIntelliJLaf.setup();
            } catch (Exception e) {
                log.error("主题设置失败", e);
            }

            // 3. FlatLaf 统一商务风格属性（圆角、阴影等）
            BusinessStyleUtils.applyBusinessStyle();

            IconFontSwing.register(FontAwesome.getIconFont());

            // 4. 启动
            final MainFrame mainFrame = MainFrame.getInstance();
            mainFrame.initComponents();
        });

        // 5. 设置全局异常处理器，防止程序因未捕获异常崩溃
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception in thread: {}", thread.getName(), throwable);
            JOptionPane.showMessageDialog(null,
                    "发生意外错误，请查看日志文件获取详情。",
                    "错误提示", JOptionPane.ERROR_MESSAGE);
        });
    }
}
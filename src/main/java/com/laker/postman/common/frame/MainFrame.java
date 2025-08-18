package com.laker.postman.common.frame;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.dialog.ExitDialog;
import com.laker.postman.panel.EasyPostmanMainPanel;
import com.laker.postman.util.UserSettingsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 主窗口类，继承自 JFrame。
 */
@Slf4j
public class MainFrame extends JFrame {

    // 单例模式，确保只有一个实例
    private MainFrame() {
        super(); // 调用父类构造函数
        setName("EasyPostman"); // 设置窗口名称
        setTitle("EasyPostman"); // 设置窗口标题
        setIconImage(Icons.LOGO.getImage()); // 设置窗口图标
    }

    public void initComponents() {
        setContentPane(SingletonFactory.getInstance(EasyPostmanMainPanel.class)); // 设置主面板为内容面板
        initWindowSize(); // 初始化窗口大小
        initWindowCloseListener(); // 初始化窗口关闭监听器
        initWindowStateListener(); // 初始化窗口状态监听器
        pack(); // 调整窗口大小以适应内容
        restoreWindowState(); // 恢复上次的窗口状态
    }

    private void initWindowSize() {
        // 如果已有保存的窗口状态，则跳过默认大小设置
        if (UserSettingsUtil.hasWindowState()) {
            return;
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.getWidth() > 1280) {
            setPreferredSize(new Dimension(1280, 800));
        } else if (screenSize.getWidth() > 1024) {
            setPreferredSize(new Dimension(1200, 768));
        } else {
            setPreferredSize(new Dimension(960, 640));
        }
        if (screenSize.getWidth() <= 1366) {
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    private void initWindowCloseListener() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { // 当窗口关闭时触发
                saveWindowState(); // 保存窗口状态
                ExitDialog.show();
            }

            @Override
            public void windowStateChanged(WindowEvent e) { // 当窗口状态改变时触发
                // 当窗口最大化/最小化状态改变时保存状态
                log.info("windowStateChanged");
                saveWindowState();
            }
        });
    }

    private void initWindowStateListener() {
        // 监听窗口大小和位置变化
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 窗口大小改变时保存状态
                if (isVisible()) {
                    saveWindowState();
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // 窗口位置改变时保存状态
                if (isVisible()) {
                    saveWindowState();
                }
            }
        });
    }

    private void saveWindowState() {
        try {
            // 获取当前窗口状态
            boolean isMaximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;

            if (!isMaximized) {
                // 只有在非最大化状态下才保存位置和大小
                Rectangle bounds = getBounds();
                UserSettingsUtil.saveWindowState(
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height,
                        false
                );
            } else {
                // 最大化状态下保留之前的位置和大小，只更新最大化状态
                Integer savedWidth = UserSettingsUtil.getWindowWidth();
                Integer savedHeight = UserSettingsUtil.getWindowHeight();
                Integer savedX = UserSettingsUtil.getWindowX();
                Integer savedY = UserSettingsUtil.getWindowY();

                UserSettingsUtil.saveWindowState(
                        savedX != null ? savedX : 100,
                        savedY != null ? savedY : 100,
                        savedWidth != null ? savedWidth : 1280,
                        savedHeight != null ? savedHeight : 800,
                        true
                );
            }
            log.debug("窗口状态已保存");
        } catch (Exception e) {
            log.warn("保存窗口状态失败", e);
        }
    }

    private void restoreWindowState() {
        try {
            if (UserSettingsUtil.hasWindowState()) {
                Integer x = UserSettingsUtil.getWindowX();
                Integer y = UserSettingsUtil.getWindowY();
                Integer width = UserSettingsUtil.getWindowWidth();
                Integer height = UserSettingsUtil.getWindowHeight();
                boolean isMaximized = UserSettingsUtil.isWindowMaximized();

                if (x != null && y != null && width != null && height != null) {
                    // 检查窗口位置是否在有效的屏幕范围内
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    if (x >= 0 && y >= 0 && x < screenSize.width && y < screenSize.height) {
                        setBounds(x, y, width, height);
                    } else {
                        // 如果位置无效，则居中显示
                        setSize(width, height);
                        setLocationRelativeTo(null);
                    }

                    if (isMaximized) {
                        setExtendedState(Frame.MAXIMIZED_BOTH);
                    }

                    log.debug("窗口状态已恢复: x={}, y={}, width={}, height={}, maximized={}",
                            x, y, width, height, isMaximized);
                }
            } else {
                setLocationRelativeTo(null); // 窗口居中显示
            }
        } catch (Exception e) {
            log.warn("恢复窗口状态失败", e);
            setLocationRelativeTo(null); // 发生错误时居中显示
        }
    }
}
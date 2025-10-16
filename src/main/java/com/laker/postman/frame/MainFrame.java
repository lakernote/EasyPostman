package com.laker.postman.frame;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.panel.EasyPostmanMainPanel;
import com.laker.postman.service.ExitService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
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
        setName(I18nUtil.getMessage(MessageKeys.APP_NAME)); // 设置窗口名称
        setTitle(I18nUtil.getMessage(MessageKeys.APP_NAME)); // 设置窗口标题
        setIconImage(Icons.LOGO.getImage()); // 设置窗口图标
    }

    public void initComponents() {
        setContentPane(SingletonFactory.getInstance(EasyPostmanMainPanel.class)); // 设置主面板为内容面板
        initWindowSize(); // 初始化窗口大小
        initWindowCloseListener(); // 初始化窗口关闭监听器
        initWindowStateListener(); // 初始化窗口状态监听器
        pack(); // 调整窗口大小以适应内容
        setLocationRelativeTo(null); // 设置窗口居中显示
    }

    private void initWindowSize() {
        // 如果已有保存的窗口状态，则跳过默认大小设置
        if (UserSettingsUtil.hasWindowState()) {
            restoreWindowState(); // 恢复上次的窗口状态
            return;
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(getMinWindowSize()); // 设置最小窗口大小
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
                ExitService.exit();
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
            boolean isMaximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            Dimension minSize = getMinWindowSize();
            if (!isMaximized) {
                Dimension size = getSize();
                int width = Math.max(size.width, minSize.width);
                int height = Math.max(size.height, minSize.height);
                UserSettingsUtil.saveWindowState(
                        width,
                        height,
                        false
                );
            } else {
                Integer savedWidth = UserSettingsUtil.getWindowWidth();
                Integer savedHeight = UserSettingsUtil.getWindowHeight();
                int width = savedWidth != null ? Math.max(savedWidth, minSize.width) : minSize.width;
                int height = savedHeight != null ? Math.max(savedHeight, minSize.height) : minSize.height;
                UserSettingsUtil.saveWindowState(
                        width,
                        height,
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
                Integer width = UserSettingsUtil.getWindowWidth();
                Integer height = UserSettingsUtil.getWindowHeight();
                boolean isMaximized = UserSettingsUtil.isWindowMaximized();
                setPreferredSize(new Dimension(width, height));
                if (isMaximized) {
                    setExtendedState(Frame.MAXIMIZED_BOTH);
                }
                log.debug("窗口状态已恢复: width={}, height={}, maximized={}",
                        width, height, isMaximized);
            }
        } catch (Exception e) {
            log.warn("恢复窗口状态失败", e);
        }
    }

    private Dimension getMinWindowSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.getWidth() > 1280) {
            return new Dimension(1280, 800);
        } else if (screenSize.getWidth() > 1024) {
            return new Dimension(1200, 768);
        } else {
            return new Dimension(960, 640);
        }
    }
}
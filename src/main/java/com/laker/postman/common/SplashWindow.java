package com.laker.postman.common;

import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.util.FontUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 启动欢迎窗口（Splash Window），用于主程序加载时的过渡。
 */
@Slf4j
public class SplashWindow extends JWindow {

    public static final int MIN_TIME = 1000;

    private final JLabel statusLabel;

    public SplashWindow() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(245, 247, 250));
        JLabel logoLabel = new JLabel(new ImageIcon(Icons.LOGO.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(logoLabel, BorderLayout.CENTER);

        statusLabel = new JLabel("正在启动 EasyPostman...", SwingConstants.CENTER);
        statusLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 16));
        statusLabel.setForeground(new Color(80, 120, 200));
        content.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(360, 260);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void initMainFrame() {
        SwingWorker<MainFrame, Void> worker = new SwingWorker<>() {
            @Override
            protected MainFrame doInBackground() {
                long start = System.currentTimeMillis();
                MainFrame mainFrame = MainFrame.getInstance();
                mainFrame.initComponents();
                long cost = System.currentTimeMillis() - start;
                log.info("主窗口加载耗时: {} ms", cost);
                if (cost < MIN_TIME) { // loading界面至少显示ms
                    try {
                        Thread.sleep(MIN_TIME - cost);
                    } catch (InterruptedException ignored) {
                    }
                }
                return mainFrame;
            }

            @Override
            protected void done() {
                try {
                    get().setVisible(true); // 显示主窗口
                    setVisible(false); // 隐藏启动窗口
                    dispose(); // 释放启动窗口资源
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "主窗口加载失败，请重启应用。", "错误", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        };
        worker.execute();
    }
}


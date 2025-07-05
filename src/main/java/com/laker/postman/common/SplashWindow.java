package com.laker.postman.common;

import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 启动欢迎窗口（Splash Window），用于主程序加载时的过渡。
 */
@Slf4j
public class SplashWindow extends JWindow {
    public static final int MIN_TIME = 1000; // 最小显示时间，避免闪屏
    private final JLabel statusLabel; // 状态标签，用于显示加载状态

    public SplashWindow() {
        JPanel content = getJPanel();

        // Logo
        JLabel logoLabel = new JLabel(new ImageIcon(Icons.LOGO.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(logoLabel, BorderLayout.CENTER);

        // 应用名称和版本
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);
        // 应用名称
        JLabel appNameLabel = new JLabel("EasyPostman", SwingConstants.CENTER);
        appNameLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 22));
        appNameLabel.setForeground(new Color(60, 90, 180));
        infoPanel.add(appNameLabel);
        // 版本号
        JLabel versionLabel = new JLabel(SystemUtil.getCurrentVersion(), SwingConstants.CENTER);
        versionLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 13));
        versionLabel.setForeground(new Color(120, 130, 150));
        infoPanel.add(versionLabel);
        content.add(infoPanel, BorderLayout.NORTH);

        // 状态
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 5));
        bottomPanel.setOpaque(false);
        statusLabel = new JLabel("Starting EasyPostman...", SwingConstants.CENTER);
        statusLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 15));
        statusLabel.setForeground(new Color(80, 120, 200));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        content.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(450, 280); // 设置窗口大小
        setLocationRelativeTo(null);  // 居中显示
        setBackground(new Color(0, 0, 0, 0)); // 透明背景
        setAlwaysOnTop(true); // 窗口总在最上层
        setVisible(true); // 显示窗口
    }

    private static JPanel getJPanel() {
        JPanel content = new JPanel() { // 自定义面板，绘制渐变背景和圆角
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                // 渐变色（可自定义颜色）
                GradientPaint gp = new GradientPaint(0, 0, new Color(90, 155, 255), getWidth(), getHeight(), new Color(245, 247, 250));
                g2d.setPaint(gp);
                // 圆角背景
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
                g2d.dispose();
            }
        };
        content.setLayout(new BorderLayout(0, 10)); // 使用 BorderLayout 布局
        content.setOpaque(false); // 设置透明背景
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18)); // 设置内边距
        return content;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }


    public void initMainFrame() {
        SwingWorker<MainFrame, Void> worker = new SwingWorker<>() {
            @Override
            protected MainFrame doInBackground() {
                long start = System.currentTimeMillis();
                setStatus("Loading main window...");
                setProgress(30);
                MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
                setStatus("Initializing components...");
                setProgress(60);
                mainFrame.initComponents();
                setStatus("Ready");
                setProgress(100);
                long cost = System.currentTimeMillis() - start;
                log.info("main frame initComponents cost: {} ms", cost);
                if (cost < MIN_TIME) {
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
                    setStatus("Done, showing main window...");
                    MainFrame mainFrame = get();
                    // 渐隐动画关闭 SplashWindow
                    Timer timer = new Timer(15, null);
                    timer.addActionListener(e -> {
                        float opacity = getOpacity();
                        if (opacity > 0.05f) {
                            setOpacity(Math.max(0f, opacity - 0.08f));
                        } else {
                            timer.stop();
                            setVisible(false);
                            dispose();
                            // 显示主界面
                            SwingUtilities.invokeLater(() -> {
                                mainFrame.setVisible(true);
                            });
                        }
                    });
                    timer.start();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Failed to load main window, please restart the application.", "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        };
        worker.execute();
    }
}
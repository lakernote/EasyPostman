package com.laker.postman.common;

import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.panel.EasyPostmanMainPanel;
import com.laker.postman.util.FontUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 通用的加载中面板，可自定义提示文本和进度条宽度。
 */
@Slf4j
public class EasyLoadingPanel extends JPanel {
    public static final int TIME = 1000;
    private static EasyLoadingPanel globalLoadingPanel;

    private EasyLoadingPanel() {
        this("快马加鞭加载中，请稍候…");
    }

    private EasyLoadingPanel(String text) {
        this(text, 200);
    }

    private EasyLoadingPanel(String text, int barWidth) {
        super(new GridBagLayout());
        JLabel loadingLabel = new JLabel(text);
        loadingLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 18));
        loadingLabel.setForeground(new Color(80, 120, 200));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // 设置为不确定模式
        progressBar.setPreferredSize(new Dimension(barWidth, 18));
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.add(loadingLabel);
        inner.add(Box.createVerticalStrut(18));
        inner.add(progressBar);
        add(inner, new GridBagConstraints());
        setBackground(new Color(245, 247, 250));
    }


    /**
     * 在指定 JFrame 上显示 loading 面板
     */
    public static void showOn() {
        if (globalLoadingPanel == null) {
            globalLoadingPanel = new EasyLoadingPanel();
        }
        MainFrame frame = MainFrame.getInstance();
        frame.setContentPane(globalLoadingPanel);
        frame.revalidate();
        frame.repaint();
    }

    /**
     * 隐藏 loading 面板，恢复主界面
     *
     * @param mainPanel 主内容面板
     */
    private static void hideFrom(JPanel mainPanel) {
        MainFrame frame = MainFrame.getInstance();
        frame.setContentPane(mainPanel);
        frame.revalidate();
        frame.repaint();
    }

    public static void hideFromMainPanel() {
        SwingWorker<JPanel, Void> worker = new SwingWorker<>() {
            @Override
            protected JPanel doInBackground() {
                long start = System.currentTimeMillis();
                JPanel panel = EasyPostmanMainPanel.getInstance();
                long cost = System.currentTimeMillis() - start;
                if (cost < TIME) { // loading界面至少显示ms
                    try {
                        Thread.sleep(TIME - cost);
                    } catch (InterruptedException ignored) {
                    }
                }
                return panel;
            }

            @Override
            protected void done() {
                try {
                    EasyLoadingPanel.hideFrom(get());
                } catch (Exception e) {
                    log.error("主面板加载失败", e);
                }
            }
        };
        worker.execute();
    }
}
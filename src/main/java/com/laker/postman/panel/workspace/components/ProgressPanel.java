package com.laker.postman.panel.workspace.components;

import com.laker.postman.util.EasyPostManFontUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 进度显示面板公共组件
 * 用于显示操作进度和状态信息
 */
public class ProgressPanel extends JPanel {

    @Getter
    private JProgressBar progressBar;
    @Getter
    private JLabel statusLabel;

    public ProgressPanel(String title) {
        initComponents();
        setupLayout(title);
    }

    private void initComponents() {
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("准备就绪");

        statusLabel = new JLabel("请填写配置信息");
        statusLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 11));
    }

    private void setupLayout(String title) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12)
        ));

        // 状态标签
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.NORTH);

        // 进度条
        add(progressBar, BorderLayout.CENTER);
    }

    /**
     * 更新进度
     */
    public void updateProgress(int progress, String message) {
        progressBar.setValue(progress);
        if (message != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * 设置进度条文本
     */
    public void setProgressText(String text) {
        progressBar.setString(text);
    }

    /**
     * 重置进度
     */
    public void reset() {
        progressBar.setValue(0);
        progressBar.setString("准备就绪");
        statusLabel.setText("请填写配置信息");
    }
}

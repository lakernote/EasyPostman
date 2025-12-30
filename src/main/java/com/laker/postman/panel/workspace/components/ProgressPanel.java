package com.laker.postman.panel.workspace.components;

import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
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
        progressBar.setString(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_READY));

        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_FILL_CONFIG));
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -1));
    }

    private void setupLayout(String title) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                FontsUtil.getDefaultFont(Font.BOLD)
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
     * 设置进度文本
     */
    public void setProgressText(String text) {
        progressBar.setString(text);
    }

    /**
     * 重置进度面板状态
     */
    public void reset() {
        progressBar.setValue(0);
        progressBar.setString(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_READY));
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_FILL_CONFIG));
    }
}

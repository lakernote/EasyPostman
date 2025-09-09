package com.laker.postman.service.update;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 更新进度对话框 - 显示下载进度
 */
public class UpdateProgressDialog {

    private final JDialog dialog;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JLabel sizeLabel;
    private final JLabel speedLabel;
    private final JLabel timeLabel;
    private final JButton cancelButton;

    private Runnable onCancelListener;

    public UpdateProgressDialog(JFrame parent) {
        dialog = new JDialog(parent, I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING), true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // 创建组件并直接初始化引用
        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_CONNECTING), SwingConstants.CENTER);
        progressBar = new JProgressBar(0, 100);
        sizeLabel = createInfoLabel("-- / -- MB");
        speedLabel = createInfoLabel("-- KB/s");
        timeLabel = createInfoLabel("--");
        cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_CANCEL_DOWNLOAD));

        JPanel panel = createContentPanel();
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
        panel.setBackground(new Color(248, 249, 250));
        panel.setOpaque(true);

        // 图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/download.svg", 32, 32));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(12));

        // 状态标签 - 使用已初始化的 statusLabel
        statusLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 16));
        statusLabel.setForeground(new Color(33, 37, 41));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(16));

        // 进度条 - 使用已初始化的 progressBar
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setPreferredSize(new Dimension(320, 28));
        progressBar.setMaximumSize(new Dimension(320, 28));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setBackground(new Color(240, 242, 245));
        progressBar.setForeground(new Color(33, 150, 243));
        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(16));

        // 信息面板
        JPanel infoPanel = createInfoPanel();
        panel.add(infoPanel);
        panel.add(Box.createVerticalStrut(20));

        // 取消按钮 - 使用已初始化的 cancelButton
        cancelButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 14));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(e -> {
            if (onCancelListener != null) {
                onCancelListener.run();
            }
        });
        panel.add(cancelButton);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new Dimension(320, 80));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 0, 2, 0);

        // 使用已初始化的组件
        infoPanel.add(sizeLabel, gbc);
        infoPanel.add(speedLabel, gbc);
        infoPanel.add(timeLabel, gbc);

        return infoPanel;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        label.setForeground(new Color(108, 117, 125));
        return label;
    }

    /**
     * 更新进度信息
     */
    public void updateProgress(int percentage, long downloaded, long total, double speed) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(percentage);
            progressBar.setString(percentage + "%");

            statusLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING));

            // 更新大小信息
            sizeLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_PROGRESS,
                    String.format("%.1f", downloaded / 1024.0 / 1024.0),
                    String.format("%.1f", total / 1024.0 / 1024.0)));

            // 更新速度信息
            speedLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_SPEED,
                    String.format("%.1f", speed / 1024.0)));

            // 更新时间信息
            if (speed > 0 && total > 0) {
                long remainingBytes = total - downloaded;
                int remainingSeconds = (int) (remainingBytes / speed);
                timeLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_ESTIMATED_TIME, remainingSeconds));
            } else {
                timeLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_ESTIMATED_TIME, "--"));
            }
        });
    }

    /**
     * 设置取消监听器
     */
    public void setOnCancelListener(Runnable listener) {
        this.onCancelListener = listener;
    }

    /**
     * 显示对话框
     */
    public void show() {
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }

    /**
     * 隐藏对话框
     */
    public void hide() {
        SwingUtilities.invokeLater(() -> dialog.dispose());
    }
}

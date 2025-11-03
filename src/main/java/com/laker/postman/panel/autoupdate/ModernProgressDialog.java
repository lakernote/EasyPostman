package com.laker.postman.panel.autoupdate;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化下载进度对话框 - 简洁直观的进度显示
 */
public class ModernProgressDialog {

    private final JDialog dialog;
    private final JProgressBar progressBar;
    private final JLabel percentLabel;
    private final JLabel statusLabel;
    private final JLabel sizeLabel;
    private final JLabel speedLabel;
    private final JButton cancelButton;

    private Runnable onCancelListener;

    public ModernProgressDialog(JFrame parent) {
        dialog = new JDialog(parent, I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING), true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // 初始化组件
        percentLabel = new JLabel("0%", SwingConstants.CENTER);
        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_CONNECTING), SwingConstants.CENTER);
        progressBar = new JProgressBar(0, 100);
        sizeLabel = new JLabel("-- / -- MB");
        speedLabel = new JLabel("-- KB/s");
        cancelButton = createCancelButton();

        JPanel panel = createContentPanel();
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
    }

    private JPanel createContentPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ModernColors.BG_WHITE);
        mainPanel.setBorder(new EmptyBorder(32, 40, 28, 40));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/download.svg", 48, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(iconLabel);
        contentPanel.add(Box.createVerticalStrut(16));

        // 状态文本
        statusLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 16));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // 百分比
        percentLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 32));
        percentLabel.setForeground(ModernColors.PRIMARY);
        percentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(percentLabel);
        contentPanel.add(Box.createVerticalStrut(12));

        // 进度条
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setMaximumSize(new Dimension(400, 8));
        progressBar.setStringPainted(false);
        progressBar.setBackground(ModernColors.BG_MEDIUM);
        progressBar.setForeground(ModernColors.PRIMARY);
        progressBar.setBorderPainted(false);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(progressBar);
        contentPanel.add(Box.createVerticalStrut(20));

        // 详细信息
        JPanel detailsPanel = createDetailsPanel();
        detailsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(detailsPanel);
        contentPanel.add(Box.createVerticalStrut(24));

        // 取消按钮
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(cancelButton);

        mainPanel.add(contentPanel);
        return mainPanel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 20, 8));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(400, 60));
        panel.setMaximumSize(new Dimension(400, 60));

        // 大小信息
        JLabel sizeTitle = new JLabel(I18nUtil.isChinese() ? "已下载" : "Downloaded");
        sizeTitle.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        sizeTitle.setForeground(ModernColors.TEXT_HINT);
        panel.add(sizeTitle);

        sizeLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 13));
        sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(sizeLabel);

        // 速度信息
        JLabel speedTitle = new JLabel(I18nUtil.isChinese() ? "下载速度" : "Speed");
        speedTitle.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        speedTitle.setForeground(ModernColors.TEXT_HINT);
        panel.add(speedTitle);

        speedLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 13));
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(speedLabel);

        return panel;
    }

    private JButton createCancelButton() {
        JButton button = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_CANCEL_DOWNLOAD));
        button.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        button.setForeground(ModernColors.TEXT_SECONDARY);
        button.setBackground(ModernColors.BG_LIGHT);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 24, 8, 24));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ModernColors.HOVER_BG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ModernColors.BG_LIGHT);
            }
        });

        button.addActionListener(e -> {
            if (onCancelListener != null) {
                onCancelListener.run();
            }
        });

        return button;
    }

    public void updateProgress(int percentage, long downloaded, long total, double speed) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(percentage);
            percentLabel.setText(percentage + "%");
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING));

            // 更新大小
            String downloadedStr = formatBytes(downloaded);
            String totalStr = formatBytes(total);
            sizeLabel.setText(String.format("%s / %s", downloadedStr, totalStr));

            // 更新速度
            String speedStr = formatSpeed(speed);
            speedLabel.setText(speedStr);
        });
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private String formatSpeed(double speed) {
        if (speed >= 1024 * 1024) {
            return String.format("%.2f MB/s", speed / (1024.0 * 1024.0));
        } else if (speed >= 1024) {
            return String.format("%.1f KB/s", speed / 1024.0);
        }
        return String.format("%.0f B/s", speed);
    }

    public void setOnCancelListener(Runnable listener) {
        this.onCancelListener = listener;
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            // 重置状态
            progressBar.setValue(0);
            percentLabel.setText("0%");
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_CONNECTING));
            sizeLabel.setText("-- / -- MB");
            speedLabel.setText("-- KB/s");
            dialog.setVisible(true);
        });
    }

    public void hide() {
        SwingUtilities.invokeLater(() -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
    }
}


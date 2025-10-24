package com.laker.postman.service.update;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
    private final JLabel percentageLabel;

    private Runnable onCancelListener;

    // UI 常量
    private static final Color BACKGROUND_COLOR = new Color(252, 253, 254);
    private static final Color PANEL_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY_TEXT = new Color(32, 33, 36);
    private static final Color SECONDARY_TEXT = new Color(95, 99, 104);
    private static final Color PROGRESS_COLOR = new Color(26, 115, 232);
    private static final Color PROGRESS_BACKGROUND = new Color(241, 243, 244);
    private static final Color BORDER_COLOR = new Color(218, 220, 224);

    public UpdateProgressDialog(JFrame parent) {
        dialog = new JDialog(parent, I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING), true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // 创建组件并直接初始化引用
        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_CONNECTING), SwingConstants.CENTER);
        progressBar = new JProgressBar(0, 100);
        percentageLabel = new JLabel("0%", SwingConstants.CENTER);
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
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 创建内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(PANEL_BACKGROUND);
        contentPanel.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(32, 40, 32, 40)
        ));

        // 头部区域 - 图标和状态
        JPanel headerPanel = createHeaderPanel();
        contentPanel.add(headerPanel);
        contentPanel.add(Box.createVerticalStrut(24));

        // 进度区域
        JPanel progressPanel = createProgressPanel();
        contentPanel.add(progressPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // 详细信息区域
        JPanel detailsPanel = createDetailsPanel();
        contentPanel.add(detailsPanel);
        contentPanel.add(Box.createVerticalStrut(24));

        // 按钮区域
        JPanel buttonPanel = createButtonPanel();
        contentPanel.add(buttonPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // 图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/download.svg", 40, 40));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(16));

        // 状态标签
        statusLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 18));
        statusLabel.setForeground(PRIMARY_TEXT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(statusLabel);

        return headerPanel;
    }

    private JPanel createProgressPanel() {
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.setOpaque(false);

        // 进度条容器
        JPanel progressContainer = new JPanel(new BorderLayout());
        progressContainer.setOpaque(false);
        progressContainer.setMaximumSize(new Dimension(360, 40));
        progressContainer.setPreferredSize(new Dimension(360, 40));

        // 进度条
        progressBar.setStringPainted(false); // 不显示内置文本
        progressBar.setPreferredSize(new Dimension(360, 8));
        progressBar.setBackground(PROGRESS_BACKGROUND);
        progressBar.setForeground(PROGRESS_COLOR);
        progressBar.setBorderPainted(false);

        // 百分比标签
        percentageLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 24));
        percentageLabel.setForeground(PRIMARY_TEXT);
        percentageLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        progressContainer.add(percentageLabel, BorderLayout.NORTH);
        progressContainer.add(progressBar, BorderLayout.CENTER);

        progressPanel.add(progressContainer);
        return progressPanel;
    }

    private JPanel createDetailsPanel() {
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setOpaque(false);
        detailsPanel.setMaximumSize(new Dimension(360, 120));
        detailsPanel.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));
        detailsPanel.setBackground(new Color(248, 249, 250));

        GridBagConstraints gbc = new GridBagConstraints();

        // 创建带图标的信息行 - 使用 FontAwesome 图标
        addInfoRow(detailsPanel, FontAwesome.ARCHIVE, sizeLabel, 0, gbc);
        addInfoRow(detailsPanel, FontAwesome.TACHOMETER, speedLabel, 1, gbc);
        addInfoRow(detailsPanel, FontAwesome.CLOCK_O, timeLabel, 2, gbc);

        return detailsPanel;
    }

    private void addInfoRow(JPanel parent, FontAwesome iconType, JLabel label, int row, GridBagConstraints gbc) {
        // 图标标签 - 使用 FontAwesome 图标
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(IconFontSwing.buildIcon(iconType, 14, SECONDARY_TEXT));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 12);
        gbc.weightx = 0;
        parent.add(iconLabel, gbc);

        // 信息标签
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        parent.add(label, gbc);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        // 美化取消按钮
        cancelButton.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 220, 224), 1, true),
                new EmptyBorder(10, 32, 10, 32)
        ));
        cancelButton.setBackground(Color.WHITE);
        cancelButton.setForeground(SECONDARY_TEXT);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 添加悬停效果
        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                cancelButton.setBackground(new Color(248, 249, 250));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                cancelButton.setBackground(Color.WHITE);
            }
        });

        cancelButton.addActionListener(e -> {
            if (onCancelListener != null) {
                onCancelListener.run();
            }
        });

        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        label.setForeground(SECONDARY_TEXT);
        return label;
    }

    /**
     * 更新进度信息
     */
    public void updateProgress(int percentage, long downloaded, long total, double speed) {
        SwingUtilities.invokeLater(() -> {
            // 更新进度条和百分比
            progressBar.setValue(percentage);
            percentageLabel.setText(percentage + "%");

            // 更新状态
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING));

            // 更新详细信息 - 使用更好的格式化
            updateSizeInfo(downloaded, total);
            updateSpeedInfo(speed);
            updateTimeInfo(speed, downloaded, total);
        });
    }

    private void updateSizeInfo(long downloaded, long total) {
        String downloadedStr = formatBytes(downloaded);
        String totalStr = formatBytes(total);
        sizeLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_PROGRESS, downloadedStr, totalStr));
    }

    private void updateSpeedInfo(double speed) {
        String speedStr = formatSpeed(speed);
        speedLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_SPEED, speedStr));
    }

    private void updateTimeInfo(double speed, long downloaded, long total) {
        if (speed > 0 && total > 0) {
            long remainingBytes = total - downloaded;
            int remainingSeconds = (int) (remainingBytes / speed);
            String timeStr = formatTime(remainingSeconds);
            timeLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_ESTIMATED_TIME, timeStr));
        } else {
            timeLabel.setText(I18nUtil.getMessage(MessageKeys.UPDATE_ESTIMATED_TIME, "--"));
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }

    private String formatSpeed(double speed) {
        if (speed >= 1024 * 1024) {
            return String.format("%.1f MB/s", speed / (1024.0 * 1024.0));
        } else if (speed >= 1024) {
            return String.format("%.1f KB/s", speed / 1024.0);
        } else {
            return String.format("%.0f B/s", speed);
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
        }
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

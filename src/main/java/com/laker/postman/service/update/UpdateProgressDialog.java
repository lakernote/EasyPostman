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
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = createContentPanel();
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        // 初始化组件引用
        Component[] components = findComponents(panel);
        statusLabel = (JLabel) components[0];
        progressBar = (JProgressBar) components[1];
        sizeLabel = (JLabel) components[2];
        speedLabel = (JLabel) components[3];
        timeLabel = (JLabel) components[4];
        cancelButton = (JButton) components[5];
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

        // 状态标签
        JLabel status = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_CONNECTING), SwingConstants.CENTER);
        status.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 16));
        status.setForeground(new Color(33, 37, 41));
        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(status);
        panel.add(Box.createVerticalStrut(16));

        // 进度条
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setString("0%");
        progress.setPreferredSize(new Dimension(320, 28));
        progress.setMaximumSize(new Dimension(320, 28));
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);
        progress.setBackground(new Color(240, 242, 245));
        progress.setForeground(new Color(33, 150, 243));
        panel.add(progress);
        panel.add(Box.createVerticalStrut(16));

        // 信息面板
        JPanel infoPanel = createInfoPanel();
        panel.add(infoPanel);
        panel.add(Box.createVerticalStrut(20));

        // 取消按钮
        JButton cancel = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_CANCEL_DOWNLOAD));
        cancel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 14));
        cancel.setFocusPainted(false);
        cancel.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancel.addActionListener(e -> {
            if (onCancelListener != null) {
                onCancelListener.run();
            }
        });
        panel.add(cancel);

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

        JLabel size = createInfoLabel("-- / -- MB");
        JLabel speed = createInfoLabel("-- KB/s");
        JLabel time = createInfoLabel("--");

        infoPanel.add(size, gbc);
        infoPanel.add(speed, gbc);
        infoPanel.add(time, gbc);

        return infoPanel;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        label.setForeground(new Color(108, 117, 125));
        return label;
    }

    private Component[] findComponents(Container container) {
        Component[] result = new Component[6];
        findComponentsRecursively(container, result, new int[]{0});
        return result;
    }

    private void findComponentsRecursively(Container container, Component[] result, int[] index) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && ((JLabel) comp).getFont() != null && ((JLabel) comp).getFont().isBold()) {
                if (result[0] == null) result[0] = comp; // statusLabel
            } else if (comp instanceof JProgressBar) {
                result[1] = comp; // progressBar
            } else if (comp instanceof JLabel && index[0] >= 2 && index[0] <= 4) {
                result[index[0]] = comp; // info labels
                index[0]++;
            } else if (comp instanceof JButton) {
                result[5] = comp; // cancelButton
            } else if (comp instanceof Container) {
                findComponentsRecursively((Container) comp, result, index);
            }

            if (comp instanceof JLabel && result[0] != null && result[1] != null &&
                    !((JLabel) comp).getFont().isBold() && result[2] == null) {
                result[2] = comp;
                index[0] = 3;
            } else if (comp instanceof JLabel && result[2] != null && result[3] == null &&
                    !((JLabel) comp).getFont().isBold()) {
                result[3] = comp;
                index[0] = 4;
            } else if (comp instanceof JLabel && result[3] != null && result[4] == null &&
                    !((JLabel) comp).getFont().isBold()) {
                result[4] = comp;
                index[0] = 5;
            }
        }
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
            String sizeText = String.format("%.1f / %.1f MB",
                    downloaded / 1024.0 / 1024.0,
                    total / 1024.0 / 1024.0);
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

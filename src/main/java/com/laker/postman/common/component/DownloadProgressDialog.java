package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.util.FileSizeDisplayUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * 通用下载进度对话框组件
 */
public class DownloadProgressDialog extends JDialog {
    private final JLabel detailsLabel;
    private final JLabel speedLabel;
    private final JLabel timeLabel;
    private final JButton cancelButton;
    @Getter
    private boolean cancelled = false;

    public DownloadProgressDialog(String title) {
        super((JFrame) null, title, false);
        setModal(false);
        setSize(350, 180);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/download.svg", 24, 24));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.add(iconLabel);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titlePanel.add(titleLabel);

        detailsLabel = new JLabel("Downloaded: 0 KB");
        speedLabel = new JLabel("Speed: 0 KB/s");
        timeLabel = new JLabel("Time left: Calculating...");
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 0));
        infoPanel.add(detailsLabel);
        infoPanel.add(speedLabel);
        infoPanel.add(timeLabel);

        cancelButton = new JButton("Cancel", new FlatSVGIcon("icons/cancel.svg", 16, 16));
        cancelButton.addActionListener(e -> {
            cancelled = true;
            dispose();
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(infoPanel);
        southPanel.add(Box.createVerticalStrut(10));
        southPanel.add(buttonPanel);

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public DownloadProgressDialog() {
        this("Download Progress");
    }

    /**
     * 统一更新进度信息
     *
     * @param details 进度详情（如已下载大小）
     * @param speed   速度（如 KB/s）
     * @param time    剩余时间
     */
    private void updateProgress(String details, String speed, String time) {
        detailsLabel.setText(details);
        speedLabel.setText(speed);
        timeLabel.setText(time);
    }


    /**
     * 进度驱动方法，自动计算速度、剩余时间、已下载等，并更新UI
     */
    private void updateProgress(ProgressInfo info) {
        String sizeStr = FileSizeDisplayUtil.formatDownloadSize(info.totalBytes, info.contentLength);
        double speed = info.elapsedMillis > 0 ? (info.totalBytes * 1000.0 / info.elapsedMillis) : 0;
        String speedStr = speed > 1024 * 1024 ? String.format("Speed: %.2f MB/s", speed / (1024 * 1024)) : String.format("Speed: %.2f KB/s", speed / 1024);
        String remainStr;
        if (info.contentLength > 0 && speed > 0) {
            long remainSeconds = (long) ((info.contentLength - info.totalBytes) / speed);
            if (remainSeconds > 60) {
                remainStr = String.format("Time left: %d min %d sec", remainSeconds / 60, remainSeconds % 60);
            } else {
                remainStr = String.format("Time left: %d sec", remainSeconds);
            }
        } else {
            remainStr = "Time left: Calculating...";
        }
        updateProgress(sizeStr, speedStr, remainStr);
    }

    /**
     * 判断是否需要显示弹窗
     */
    public boolean shouldShow(int contentLength) {
        if (!SettingManager.isShowDownloadProgressDialog()) {
            return false;
        }
        int threshold = SettingManager.getDownloadProgressDialogThreshold();
        return contentLength > threshold || contentLength <= 0;
    }

    /**
     * 关闭对话框
     */
    public void closeDialog() {
        dispose();
    }


    /**
     * 判断是否到达进度UI的更新周期（约每200ms更新一次）
     *
     * @param now   当前时间戳
     * @param start 起始时间戳
     * @return 是否应该更新进度UI
     */
    private boolean shouldUpdateProgress(long now, long start) {
        return (now - start > 0) && (now % 200 < 50);
    }

    /**
     * 线程安全地更新进度（自动节流、自动切换到Swing线程）
     *
     * @param info        进度信息
     * @param startMillis 下载起始时间戳（用于节流）
     */
    public void updateProgressThreadSafe(ProgressInfo info, long startMillis) {
        long now = System.currentTimeMillis();
        if (!shouldUpdateProgress(now, startMillis)) return;
        if (SwingUtilities.isEventDispatchThread()) {
            updateProgress(info);
        } else {
            SwingUtilities.invokeLater(() -> updateProgress(info));
        }
    }
}
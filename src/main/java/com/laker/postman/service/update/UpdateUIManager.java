package com.laker.postman.service.update;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.function.Consumer;

/**
 * 更新UI管理器 - 负责所有更新相关的用户界面
 */
@Slf4j
public class UpdateUIManager {

    private static final String RELEASE_URL = "https://gitee.com/lakernote/easy-postman/releases";

    private final VersionChecker versionChecker;
    private final UpdateDownloader downloader;

    public UpdateUIManager() {
        this.versionChecker = new VersionChecker();
        this.downloader = new UpdateDownloader();
    }

    /**
     * 显示后台更新通知（右下角弹窗）
     */
    public void showUpdateNotification(UpdateInfo updateInfo) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
            if (!mainFrame.isVisible()) {
                return;
            }

            UpdateNotificationWindow notification = new UpdateNotificationWindow(
                    mainFrame, updateInfo, this::showUpdateDialog);
            notification.show();
        });
    }

    /**
     * 显示更新对话框（带更新日志）
     */
    public void showUpdateDialog(UpdateInfo updateInfo) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);

            // 使用新的带更新日志的对话框
            int choice = UpdateDialogWithChangelog.showUpdateDialog(mainFrame, updateInfo);

            // 处理用户选择: 0=手动下载, 1=自动下载, 2=稍后提醒, -1=关闭对话框
            switch (choice) {
                case 0 -> openManualDownloadPage();
                case 1 -> startAutomaticUpdate(updateInfo);
                case 2, -1 -> { /* 用户稍后提醒或关闭对话框 */ }
            }
        });
    }

    /**
     * 打开手动下载页面
     */
    private void openManualDownloadPage() {
        try {
            Desktop.getDesktop().browse(new URI(RELEASE_URL));
        } catch (Exception ex) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()));
        }
    }

    /**
     * 启动自动更新
     */
    private void startAutomaticUpdate(UpdateInfo updateInfo) {
        String downloadUrl = versionChecker.getDownloadUrl(updateInfo.getReleaseInfo());
        if (downloadUrl == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        UpdateProgressDialog progressDialog = new UpdateProgressDialog(
                SingletonFactory.getInstance(MainFrame.class));
        progressDialog.show();

        downloader.downloadAsync(downloadUrl, new UpdateDownloader.DownloadProgressCallback() {
            @Override
            public void onProgress(int percentage, long downloaded, long total, double speed) {
                SwingUtilities.invokeLater(() -> progressDialog.updateProgress(percentage, downloaded, total, speed));
            }

            @Override
            public void onCompleted(File downloadedFile) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.hide();
                    showInstallPrompt(downloadedFile);
                });
            }

            @Override
            public void onError(String errorMessage) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.hide();
                    NotificationUtil.showError(errorMessage);
                });
            }

            @Override
            public void onCancelled() {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.hide();
                    NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_CANCELLED));
                });
            }
        });

        // 设置取消按钮的回调
        progressDialog.setOnCancelListener(() -> downloader.cancel());
    }

    /**
     * 显示安装提示
     */
    private void showInstallPrompt(File installerFile) {
        String message = I18nUtil.getMessage(MessageKeys.UPDATE_INSTALL_PROMPT);
        int choice = JOptionPane.showConfirmDialog(null, message,
                I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING),
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            downloader.installUpdate(installerFile, success -> {
                if (success) {
                    System.exit(0);
                } else {
                    JOptionPane.showMessageDialog(null,
                            I18nUtil.getMessage(MessageKeys.UPDATE_OPEN_INSTALLER_FAILED, "Unknown error"),
                            I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING),
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    /**
     * 更新通知窗口
     */
    private static class UpdateNotificationWindow {
        private final JWindow window;
        private final Timer autoCloseTimer;

        public UpdateNotificationWindow(MainFrame parent, UpdateInfo updateInfo, Consumer<UpdateInfo> onUpdateClick) {
            window = new JWindow(parent);
            window.setAlwaysOnTop(true);

            JPanel panel = createNotificationPanel(updateInfo, onUpdateClick);
            window.setContentPane(panel);
            window.pack();

            // 定位到主界面右下角
            positionWindow(parent);

            // 10秒后自动关闭
            autoCloseTimer = new Timer(10000, e -> window.dispose());
            autoCloseTimer.setRepeats(false);
        }

        private void positionWindow(MainFrame parent) {
            Rectangle parentBounds = parent.getBounds();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            int x = parentBounds.x + parentBounds.width - window.getWidth() - 20;
            int y = parentBounds.y + parentBounds.height - window.getHeight() - 20;

            // 确保不超出屏幕边界
            x = Math.max(10, Math.min(x, screenSize.width - window.getWidth() - 10));
            y = Math.max(10, Math.min(y, screenSize.height - window.getHeight() - 10));

            window.setLocation(x, y);
        }

        private JPanel createNotificationPanel(UpdateInfo updateInfo, Consumer<UpdateInfo> onUpdateClick) {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
            panel.setBackground(new Color(248, 249, 250));
            // 增加面板高度，确保内容完整显示
            panel.setPreferredSize(new Dimension(400, 170));  // 从 150 增加到 170
            panel.setMinimumSize(new Dimension(400, 140));    // 从 120 增加到 140
            panel.setMaximumSize(new Dimension(400, 220));    // 从 200 增加到 220

            // 头部面板
            JPanel headerPanel = createHeaderPanel();

            // 内容面板：版本信息 + 更新摘要
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);

            // 版本信息
            JLabel versionLabel = new JLabel(String.format("%s → %s",
                    updateInfo.getCurrentVersion(), updateInfo.getLatestVersion()));
            versionLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
            versionLabel.setForeground(new Color(0, 120, 215));
            versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 更新摘要（使用 JTextArea 确保正确换行）
            String summary = extractChangelogSummary(updateInfo.getReleaseInfo(), 150);
            JTextArea summaryArea = new JTextArea(summary);
            summaryArea.setEditable(false);
            summaryArea.setFocusable(false);
            summaryArea.setLineWrap(true);          // 启用自动换行
            summaryArea.setWrapStyleWord(true);     // 在单词边界换行
            summaryArea.setRows(3);                 // 最多显示3行
            summaryArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
            summaryArea.setForeground(new Color(108, 117, 125));
            summaryArea.setBackground(new Color(248, 249, 250));  // 与面板背景一致
            summaryArea.setBorder(null);            // 移除边框
            summaryArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            // 增加最大高度，确保 3 行文字完整显示（约 18px/行 * 3 = 54px，留余量到 70px）
            summaryArea.setMaximumSize(new Dimension(360, 70));

            contentPanel.add(versionLabel);
            contentPanel.add(Box.createVerticalStrut(8));
            contentPanel.add(summaryArea);

            // 按钮面板
            JPanel buttonPanel = createButtonPanel(updateInfo, onUpdateClick);

            panel.add(headerPanel, BorderLayout.NORTH);
            panel.add(contentPanel, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            return panel;
        }

        /**
         * 提取更新日志摘要（用于通知窗口）
         */
        private String extractChangelogSummary(cn.hutool.json.JSONObject releaseInfo, int maxLength) {
            if (releaseInfo == null) {
                return getDefaultSummary();
            }

            String body = releaseInfo.getStr("body");
            if (cn.hutool.core.util.StrUtil.isBlank(body)) {
                return getDefaultSummary();
            }

            // 清理 Markdown 格式
            String cleaned = body.trim()
                    .replaceAll("^#{1,6}\\s+", "")  // 移除标题标记
                    .replaceAll("\\*\\*(.+?)\\*\\*", "$1")  // 移除粗体
                    .replaceAll("\\*(.+?)\\*", "$1")  // 移除斜体
                    .replaceAll("```[\\s\\S]*?```", "")  // 移除代码块
                    .replaceAll("`(.+?)`", "$1")  // 移除行内代码
                    .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1")  // 链接只保留文本
                    .replaceAll("\\n+", " ")  // 将所有换行替换为空格
                    .replaceAll("\\s+", " ")  // 压缩多个空格为单个空格
                    .trim();

            // 智能截断：在单词边界处截断
            if (cleaned.length() > maxLength) {
                String truncated = cleaned.substring(0, maxLength);
                int lastSpace = truncated.lastIndexOf(' ');
                if (lastSpace > maxLength * 0.8) {  // 如果最后一个空格不是太靠前
                    truncated = truncated.substring(0, lastSpace);
                }
                return truncated + "...";
            }

            return cleaned.isEmpty() ? getDefaultSummary() : cleaned;
        }

        /**
         * 获取默认摘要文本
         */
        private String getDefaultSummary() {
            return I18nUtil.isChinese() ? "点击查看更新详情..." : "Click to view update details...";
        }


        private JPanel createHeaderPanel() {
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);

            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titlePanel.setOpaque(false);

            JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/info.svg", 24, 24));
            JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
            titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));
            titleLabel.setForeground(new Color(33, 37, 41));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

            titlePanel.add(iconLabel);
            titlePanel.add(titleLabel);

            JButton closeButton = new JButton("×");
            closeButton.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));
            closeButton.setFocusPainted(false);
            closeButton.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            closeButton.setBackground(new Color(248, 249, 250));
            closeButton.setForeground(new Color(108, 117, 125));
            closeButton.addActionListener(e -> window.dispose());

            headerPanel.add(titlePanel, BorderLayout.WEST);
            headerPanel.add(closeButton, BorderLayout.EAST);

            return headerPanel;
        }

        private JPanel createButtonPanel(UpdateInfo updateInfo, Consumer<UpdateInfo> onUpdateClick) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            buttonPanel.setOpaque(false);

            JButton laterButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
            laterButton.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
            laterButton.setFocusPainted(false);
            laterButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            laterButton.setBackground(new Color(248, 249, 250));
            laterButton.addActionListener(e -> window.dispose());

            JButton updateButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_NOW));
            updateButton.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
            updateButton.setFocusPainted(false);
            updateButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            updateButton.setBackground(new Color(0, 123, 255));
            updateButton.setForeground(Color.WHITE);
            updateButton.addActionListener(e -> {
                window.dispose();
                onUpdateClick.accept(updateInfo);
            });

            buttonPanel.add(laterButton);
            buttonPanel.add(updateButton);

            return buttonPanel;
        }

        public void show() {
            window.setVisible(true);
            autoCloseTimer.start();
        }
    }
}

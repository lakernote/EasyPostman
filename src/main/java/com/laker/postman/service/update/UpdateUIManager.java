package com.laker.postman.service.update;


import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.panel.autoupdate.ModernProgressDialog;
import com.laker.postman.panel.autoupdate.ModernUpdateDialog;
import com.laker.postman.panel.autoupdate.ModernUpdateNotification;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;

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
     * 显示后台更新通知（右下角弹窗）- 使用现代化通知
     */
    public void showUpdateNotification(UpdateInfo updateInfo) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
            if (!mainFrame.isVisible()) {
                return;
            }

            ModernUpdateNotification notification = new ModernUpdateNotification(
                    mainFrame, updateInfo, this::showUpdateDialog);
            notification.show();
        });
    }

    /**
     * 显示更新对话框（带更新日志）- 使用现代化对话框
     */
    public void showUpdateDialog(UpdateInfo updateInfo) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);

            // 使用现代化更新对话框
            int choice = ModernUpdateDialog.showUpdateDialog(mainFrame, updateInfo);

            // 处理用户选择: 0=手动下载, 1=自动更新, 2=稍后提醒, -1=关闭对话框
            switch (choice) {
                case 0 -> openManualDownloadPage();
                case 1 -> startAutomaticUpdate(updateInfo);
                case 2, -1 -> { /* 用户稍后提醒或关闭对话框 */ }
                default -> log.debug("Unknown dialog choice: {}", choice);
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
     * 启动自动更新 - 使用现代化进度对话框
     */
    private void startAutomaticUpdate(UpdateInfo updateInfo) {
        String downloadUrl = versionChecker.getDownloadUrl(updateInfo.getReleaseInfo());
        if (downloadUrl == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        ModernProgressDialog progressDialog = new ModernProgressDialog(
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
        String fileName = installerFile.getName().toLowerCase();
        boolean isJarUpdate = fileName.endsWith(".jar");

        // 根据文件类型显示不同的提示消息
        String message;
        if (isJarUpdate) {
            // JAR 更新：自动替换并重启
            message = I18nUtil.isChinese()
                    ? "下载完成！应用将自动更新并重启。\n\n是否现在更新？"
                    : "Download complete! The application will update and restart automatically.\n\nUpdate now?";
        } else {
            // 安装包更新：需要手动安装
            message = I18nUtil.getMessage(MessageKeys.UPDATE_INSTALL_PROMPT);
        }

        int choice = JOptionPane.showConfirmDialog(
                SingletonFactory.getInstance(MainFrame.class),
                message,
                I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            if (isJarUpdate) {
                // JAR 更新：显示进度提示
                showUpdatingMessage();
            }

            downloader.installUpdate(installerFile, success -> {
                if (!success) {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = isJarUpdate
                                ? (I18nUtil.isChinese()
                                ? "更新失败，请手动下载最新版本。"
                                : "Update failed. Please download the latest version manually.")
                                : I18nUtil.getMessage(MessageKeys.UPDATE_OPEN_INSTALLER_FAILED, "Unknown error");

                        JOptionPane.showMessageDialog(
                                SingletonFactory.getInstance(MainFrame.class),
                                errorMsg,
                                I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING),
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
                // 成功时：JAR 更新会自动退出；安装包更新需要用户手动操作
            });
        }
    }

    /**
     * 显示正在更新的消息
     */
    private void showUpdatingMessage() {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class),
                    I18nUtil.isChinese() ? "正在更新..." : "Updating...",
                    false);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

            JLabel messageLabel = new JLabel(
                    I18nUtil.isChinese()
                            ? "正在更新应用，请稍候..."
                            : "Updating application, please wait...");
            messageLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            panel.add(messageLabel, BorderLayout.NORTH);
            panel.add(progressBar, BorderLayout.CENTER);

            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setVisible(true);
        });
    }
}

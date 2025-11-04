package com.laker.postman.service.update;


import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.panel.update.ModernProgressDialog;
import com.laker.postman.panel.update.ModernUpdateDialog;
import com.laker.postman.panel.update.ModernUpdateNotification;
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
        progressDialog.setOnCancelListener(downloader::cancel);
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
            downloader.installUpdate(installerFile, success -> {
                if (!success) {
                    SwingUtilities.invokeLater(() -> {
                        NotificationUtil.showError(I18nUtil.isChinese()
                                ? "更新失败，请手动下载最新版本。"
                                : "Update failed. Please download the latest version manually.");
                    });
                }
            });
        }
    }
}

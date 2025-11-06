package com.laker.postman.service.update;

import cn.hutool.json.JSONArray;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.model.UpdateType;
import com.laker.postman.panel.update.ModernProgressDialog;
import com.laker.postman.panel.update.ModernUpdateDialog;
import com.laker.postman.panel.update.ModernUpdateNotification;
import com.laker.postman.service.update.asset.PlatformDownloadUrlResolver;
import com.laker.postman.service.update.source.UpdateSource;
import com.laker.postman.service.update.source.UpdateSourceSelector;
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

    private final UpdateDownloader downloader;
    private final UpdateSourceSelector sourceSelector;
    private final PlatformDownloadUrlResolver downloadUrlResolver;

    public UpdateUIManager() {
        this.downloader = new UpdateDownloader();
        this.sourceSelector = new UpdateSourceSelector();
        this.downloadUrlResolver = new PlatformDownloadUrlResolver();
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
                case 1 -> showUpdateTypeSelectionAndStart(updateInfo);
                case 2, -1 -> { /* 用户稍后提醒或关闭对话框 */ }
                default -> log.debug("Unknown dialog choice: {}", choice);
            }
        });
    }

    /**
     * 打开手动下载页面 - 动态选择最佳更新源
     */
    private void openManualDownloadPage() {
        try {
            // 选择最佳更新源
            UpdateSource source = sourceSelector.selectBestSource();
            String releaseUrl = source.getWebUrl();

            log.info("Opening manual download page from {}: {}", source.getName(), releaseUrl);
            Desktop.getDesktop().browse(new URI(releaseUrl));
        } catch (Exception ex) {
            log.error("Failed to open manual download page", ex);
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()));
        }
    }

    /**
     * 显示更新类型选择对话框并开始更新
     */
    private void showUpdateTypeSelectionAndStart(UpdateInfo updateInfo) {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);

        // 检查可用的更新类型
        JSONArray assets = updateInfo.getReleaseInfo() != null ?
            updateInfo.getReleaseInfo().getJSONArray("assets") : null;

        if (assets == null || assets.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        // 检查哪些更新类型可用
        String jarUrl = downloadUrlResolver.resolveDownloadUrl(assets, UpdateType.INCREMENTAL);
        String installerUrl = downloadUrlResolver.resolveDownloadUrl(assets, UpdateType.FULL);

        if (jarUrl == null && installerUrl == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        // 如果两种更新方式都可用，让用户选择
        UpdateType selectedType;
        if (jarUrl != null && installerUrl != null) {
            selectedType = showUpdateTypeDialog(mainFrame);
            if (selectedType == null) {
                // 用户取消选择
                return;
            }
        } else if (jarUrl != null) {
            // 只有增量更新可用
            selectedType = UpdateType.INCREMENTAL;
            // 显示警告
            String warningMessage = "<html><body style='width: 350px; padding: 10px;'>" +
                    "<p style='color: #FF6B00; font-size: 11px; margin-bottom: 10px;'>" +
                    "<b>" + I18nUtil.getMessage(MessageKeys.UPDATE_INCREMENTAL_WARNING) + "</b>" +
                    "</p>" +
                    "<p style='font-size: 11px;'>" +
                    I18nUtil.getMessage(MessageKeys.UPDATE_TYPE_SELECT_MESSAGE) +
                    "</p>" +
                    "</body></html>";

            int confirm = JOptionPane.showConfirmDialog(
                mainFrame,
                warningMessage,
                I18nUtil.getMessage(MessageKeys.UPDATE_TYPE_SELECT_TITLE),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
        } else {
            // 只有全量更新可用
            selectedType = UpdateType.FULL;
        }

        // 获取对应类型的下载URL并开始更新
        String downloadUrl = selectedType == UpdateType.INCREMENTAL ? jarUrl : installerUrl;
        startAutomaticUpdate(downloadUrl, selectedType);
    }

    /**
     * 显示更新类型选择对话框
     *
     * @return 选择的更新类型，如果用户取消则返回 null
     */
    private UpdateType showUpdateTypeDialog(MainFrame parent) {
        String[] options = {
            I18nUtil.getMessage(MessageKeys.UPDATE_TYPE_INCREMENTAL),
            I18nUtil.getMessage(MessageKeys.UPDATE_TYPE_FULL)
        };

        // 使用 HTML 格式来优化显示效果
        String warningText = I18nUtil.getMessage(MessageKeys.UPDATE_INCREMENTAL_WARNING);
        String recommendedText = I18nUtil.getMessage(MessageKeys.UPDATE_FULL_RECOMMENDED);

        String htmlMessage = "<html><body style='width: 400px; padding: 10px;'>" +
                             "<p style='font-size: 12px; margin-bottom: 15px;'>" +
                             I18nUtil.getMessage(MessageKeys.UPDATE_TYPE_SELECT_MESSAGE) +
                             "</p>" +
                             "<p style='font-size: 11px; color: #FF6B00; margin-bottom: 10px;'>" +
                             "<b>" + warningText + "</b>" +
                             "</p>" +
                             "<p style='font-size: 11px; color: #008000; margin-bottom: 5px;'>" +
                             "<b>" + recommendedText + "</b>" +
                             "</p>" +
                             "</body></html>";

        int choice = JOptionPane.showOptionDialog(
            parent,
            htmlMessage,
            I18nUtil.getMessage(MessageKeys.UPDATE_TYPE_SELECT_TITLE),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1] // 默认选择全量更新
        );

        if (choice == 0) {
            return UpdateType.INCREMENTAL;
        } else if (choice == 1) {
            return UpdateType.FULL;
        } else {
            return null; // 用户取消
        }
    }

    /**
     * 启动自动更新 - 使用现代化进度对话框
     *
     * @param downloadUrl 下载链接
     * @param updateType 更新类型
     */
    private void startAutomaticUpdate(String downloadUrl, UpdateType updateType) {
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
                    showInstallPrompt(downloadedFile, updateType);
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
     *
     * @param installerFile 下载的文件
     * @param updateType 更新类型
     */
    private void showInstallPrompt(File installerFile, UpdateType updateType) {
        String fileName = installerFile.getName().toLowerCase();
        boolean isJarUpdate = fileName.endsWith(".jar") || updateType == UpdateType.INCREMENTAL;

        // 根据更新类型显示不同的提示消息
        String message;
        if (isJarUpdate) {
            // JAR 增量更新：自动替换并重启，带警告
            message = I18nUtil.getMessage(MessageKeys.UPDATE_JAR_INSTALL_PROMPT);
        } else {
            // 安装包全量更新：需要手动安装
            message = I18nUtil.getMessage(MessageKeys.UPDATE_INSTALLER_INSTALL_PROMPT);
        }

        int choice = JOptionPane.showConfirmDialog(
                SingletonFactory.getInstance(MainFrame.class),
                message,
                I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING),
                JOptionPane.YES_NO_OPTION,
                isJarUpdate ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

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


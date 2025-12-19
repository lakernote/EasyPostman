package com.laker.postman.service.update;

import cn.hutool.json.JSONArray;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.panel.update.AutoUpdateNotification;
import com.laker.postman.panel.update.ModernProgressDialog;
import com.laker.postman.panel.update.ModernUpdateDialog;
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
     * 显示后台更新通知（右下角弹窗）
     */
    public void showUpdateNotification(UpdateInfo updateInfo) {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        AutoUpdateNotification.show(mainFrame, updateInfo, this::showUpdateDialog);
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
     * 开始全量静默升级
     */
    private void showUpdateTypeSelectionAndStart(UpdateInfo updateInfo) {

        // 检查可用的安装包
        JSONArray assets = updateInfo.getReleaseInfo() != null ?
                updateInfo.getReleaseInfo().getJSONArray("assets") : null;

        if (assets == null || assets.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        // 获取平台特定的安装包下载链接
        String installerUrl = downloadUrlResolver.resolveDownloadUrl(assets);

        if (installerUrl == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.UPDATE_NO_INSTALLER_FOUND));
            return;
        }

        // 直接开始全量更新
        startAutomaticUpdate(installerUrl);
    }

    /**
     * 启动自动更新 - 使用现代化进度对话框
     *
     * @param downloadUrl 下载链接
     */
    private void startAutomaticUpdate(String downloadUrl) {
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
                SwingUtilities.invokeLater(() -> progressDialog.hide());
                NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_CANCELLED));
            }
        });

        // 设置取消按钮的回调
        progressDialog.setOnCancelListener(downloader::cancel);
    }

    /**
     * 显示安装提示并静默安装
     *
     * @param installerFile 下载的安装包文件
     */
    private void showInstallPrompt(File installerFile) {
        // 安装包全量更新：静默安装
        String message = I18nUtil.getMessage(MessageKeys.UPDATE_INSTALLER_INSTALL_PROMPT);

        int choice = JOptionPane.showConfirmDialog(
                SingletonFactory.getInstance(MainFrame.class),
                message,
                I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOADING),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            downloader.installUpdate(installerFile, success -> {
                if (!success) {
                    SwingUtilities.invokeLater(() -> NotificationUtil.showError(I18nUtil.isChinese()
                            ? "更新失败，请手动下载最新版本。"
                            : "Update failed. Please download the latest version manually."));
                }
            });
        }
    }
}


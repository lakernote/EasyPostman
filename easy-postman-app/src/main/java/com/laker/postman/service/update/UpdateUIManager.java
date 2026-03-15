package com.laker.postman.service.update;

import cn.hutool.json.JSONArray;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.panel.update.AutoUpdateNotification;
import com.laker.postman.panel.update.ModernProgressDialog;
import com.laker.postman.panel.update.ModernUpdateDialog;
import com.laker.postman.panel.update.NoAssetDialog;
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
     * 显示有新版本但无安装包的提示对话框（手动检查时直接弹出）
     */
    public void showNoAssetDialog(UpdateInfo updateInfo) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
            boolean goToGitHub = NoAssetDialog.show(mainFrame, updateInfo);
            if (goToGitHub) {
                openGitHubReleasePage(updateInfo.getLatestVersion());
            }
        });
    }

    /**
     * 后台检查发现新版本但无安装包时，先显示右下角 toast；
     * 用户点击「前往 GitHub」后再弹 NoAssetDialog
     */
    public void showNoAssetNotification(UpdateInfo updateInfo) {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        AutoUpdateNotification.showNoAsset(mainFrame, updateInfo, this::showNoAssetDialog);
    }

    /**
     * 打开 GitHub 发布页（指定版本）
     */
    private void openGitHubReleasePage(String version) {
        try {
            String url = "https://github.com/lakernote/easy-postman/releases/tag/" + version;
            log.info("Opening GitHub release page: {}", url);
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            log.error("Failed to open GitHub release page", ex);
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, ex.getMessage()));
        }
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
            // 安装包暂不可用，用现代化对话框引导用户去 GitHub 手动下载
            log.warn("No installer URL found for current platform, showing NoAssetDialog");
            boolean goToGitHub = NoAssetDialog.show(
                    SingletonFactory.getInstance(MainFrame.class), updateInfo);
            if (goToGitHub) {
                openGitHubReleasePage(updateInfo.getLatestVersion());
            }
            return;
        }

        // 直接开始全量更新
        startAutomaticUpdate(installerUrl);
    }

    /**
     * 启动自动更新 - 使用现代化进度对话框
     *
     * <p>关键顺序：
     * <ol>
     *   <li>先在后台线程启动下载（downloader.downloadAsync），此时进度回调已就绪</li>
     *   <li>注册 cancelListener（在 show() 阻塞 EDT 之前完成）</li>
     *   <li>最后调用 progressDialog.show()，它会在 EDT 上 modal-block，
     *       直到 hide() 被下载回调调用才解除</li>
     * </ol>
     * 这样进度回调的 invokeLater 就能正常排进 EDT 队列，进度条才会滚动。
     * </p>
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

        // 1. 先注册取消回调（在 show() 阻塞 EDT 之前）
        progressDialog.setOnCancelListener(downloader::cancel);

        // 2. 先在后台线程启动下载
        downloader.downloadAsync(downloadUrl, new UpdateDownloader.DownloadProgressCallback() {
            @Override
            public void onProgress(int percentage, long downloaded, long total, double speed) {
                // show() 阻塞 EDT，invokeLater 排队后等 EDT 空出来就会执行
                progressDialog.updateProgress(percentage, downloaded, total, speed);
            }

            @Override
            public void onCompleted(File downloadedFile) {
                progressDialog.hide(); // 解除 modal 阻塞
                SwingUtilities.invokeLater(() -> showInstallPrompt(downloadedFile));
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.hide(); // 解除 modal 阻塞
                SwingUtilities.invokeLater(() -> NotificationUtil.showError(errorMessage));
            }

            @Override
            public void onCancelled() {
                // triggerCancel() 已经调了 hideNow()，这里只需通知
                SwingUtilities.invokeLater(() ->
                        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_CANCELLED)));
            }
        });

        // 3. 最后 show() — modal 阻塞 EDT，直到 hide() 被调用才返回
        //    此方法本身已经在 EDT 上（由 showUpdateTypeSelectionAndStart -> SwingUtilities.invokeLater 保证）
        progressDialog.show();
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


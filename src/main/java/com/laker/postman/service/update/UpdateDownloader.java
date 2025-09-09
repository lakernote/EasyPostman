package com.laker.postman.service.update;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 文件下载管理器 - 负责下载更新文件
 */
@Slf4j
public class UpdateDownloader {

    /**
     * 下载进度回调接口
     */
    public interface DownloadProgressCallback {
        void onProgress(int percentage, long downloaded, long total, double speed);

        void onCompleted(File downloadedFile);

        void onError(String errorMessage);

        void onCancelled();
    }

    private volatile boolean cancelled = false;

    /**
     * 异步下载文件
     */
    public CompletableFuture<File> downloadAsync(String downloadUrl, DownloadProgressCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadFile(downloadUrl, callback);
            } catch (Exception e) {
                if (!cancelled) {
                    String friendlyError = getFriendlyErrorMessage(e);
                    callback.onError(friendlyError);
                }
                return null;
            }
        });
    }

    /**
     * 取消下载
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * 下载文件的核心方法
     */
    private File downloadFile(String downloadUrl, DownloadProgressCallback callback) throws IOException {
        URL url = new URL(downloadUrl);
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        File tempFile = File.createTempFile("EasyPostman-", "-" + fileName);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "EasyPostman-Updater");

        long totalSize = conn.getContentLength();

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;
            long startTime = System.currentTimeMillis();
            long lastProgressTime = 0;

            while ((bytesRead = in.read(buffer)) != -1 && !cancelled) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                // 限制进度回调频率，避免UI卡顿
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastProgressTime > 100) { // 每100ms更新一次
                    double elapsedSeconds = (currentTime - startTime) / 1000.0;
                    double speed = elapsedSeconds > 0 ? downloaded / elapsedSeconds : 0;
                    int percentage = totalSize > 0 ? (int) ((downloaded * 100L) / totalSize) : 0;

                    callback.onProgress(percentage, downloaded, totalSize, speed);
                    lastProgressTime = currentTime;
                }
            }
        }

        if (cancelled) {
            tempFile.delete();
            callback.onCancelled();
            return null;
        }

        callback.onCompleted(tempFile);
        return tempFile;
    }

    /**
     * 将技术异常转换为用户友好的错误消息
     */
    private String getFriendlyErrorMessage(Exception ex) {
        if (ex instanceof java.net.SocketTimeoutException) {
            return I18nUtil.getMessage(MessageKeys.ERROR_NETWORK_TIMEOUT);
        } else if (ex instanceof java.net.UnknownHostException) {
            return I18nUtil.getMessage(MessageKeys.ERROR_SERVER_UNREACHABLE);
        } else if (ex instanceof java.io.FileNotFoundException) {
            return I18nUtil.getMessage(MessageKeys.ERROR_INVALID_DOWNLOAD_LINK);
        } else if (ex instanceof java.io.IOException) {
            String message = ex.getMessage();
            if (message != null) {
                if (message.contains("No space left on device")) {
                    return I18nUtil.getMessage(MessageKeys.ERROR_DISK_SPACE_INSUFFICIENT);
                } else if (message.contains("Permission denied")) {
                    return I18nUtil.getMessage(MessageKeys.ERROR_PERMISSION_DENIED);
                }
            }
            return I18nUtil.getMessage(MessageKeys.ERROR_IO_EXCEPTION, message);
        }
        return I18nUtil.getMessage(MessageKeys.UPDATE_DOWNLOAD_FAILED, ex.getMessage());
    }

    /**
     * 安装下载的文件
     */
    public void installUpdate(File installerFile, Consumer<Boolean> callback) {
        try {
            Desktop.getDesktop().open(installerFile);
            callback.accept(true);
        } catch (Exception e) {
            log.error("Failed to open installer", e);
            callback.accept(false);
        }
    }
}
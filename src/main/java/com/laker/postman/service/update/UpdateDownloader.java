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

    private static final String GITHUB_COM = "github.com";
    private static final String GITEE_COM = "gitee.com";

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
     * 异步下载文件（支持多源切换）
     */
    public CompletableFuture<File> downloadAsync(String downloadUrl, DownloadProgressCallback callback) {
        // 重置取消标志，确保新的下载任务从干净的状态开始
        cancelled = false;

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 尝试从原始 URL 下载
                log.info("Attempting to download from: {}", downloadUrl);
                return downloadFile(downloadUrl, callback);
            } catch (Exception e) {
                if (!cancelled) {
                    log.warn("Download failed from primary source: {}", e.getMessage());

                    // 尝试切换到备用源
                    String alternativeUrl = getAlternativeDownloadUrl(downloadUrl);
                    if (alternativeUrl != null && !alternativeUrl.equals(downloadUrl)) {
                        log.info("Trying alternative source: {}", alternativeUrl);
                        callback.onProgress(0, 0, 0, 0); // 重置进度

                        try {
                            return downloadFile(alternativeUrl, callback);
                        } catch (Exception e2) {
                            log.error("Download failed from alternative source: {}", e2.getMessage());
                            String friendlyError = getFriendlyErrorMessage(e2);
                            callback.onError(friendlyError);
                        }
                    } else {
                        String friendlyError = getFriendlyErrorMessage(e);
                        callback.onError(friendlyError);
                    }
                }
                return null;
            }
        });
    }

    /**
     * 获取备用下载源
     * GitHub <-> Gitee 互为备用
     */
    private String getAlternativeDownloadUrl(String originalUrl) {
        if (originalUrl == null) {
            return null;
        }

        // GitHub -> Gitee
        if (originalUrl.contains(GITHUB_COM) || originalUrl.contains("githubusercontent.com")) {
            // GitHub Release 下载链接格式:
            // https://github.com/lakernote/easy-postman/releases/download/v1.0.0/EasyPostman-1.0.0.msi
            // Gitee Release 下载链接格式:
            // https://gitee.com/lakernote/easy-postman/releases/download/v1.0.0/EasyPostman-1.0.0.msi

            String giteeUrl = originalUrl
                    .replace(GITHUB_COM, GITEE_COM)
                    .replace("githubusercontent.com", GITEE_COM);

            log.debug("Switched from GitHub to Gitee: {} -> {}", originalUrl, giteeUrl);
            return giteeUrl;
        }

        // Gitee -> GitHub
        if (originalUrl.contains(GITEE_COM)) {
            String githubUrl = originalUrl.replace(GITEE_COM, GITHUB_COM);
            log.debug("Switched from Gitee to GitHub: {} -> {}", originalUrl, githubUrl);
            return githubUrl;
        }

        return null;
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

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);  // 连接超时 15 秒
            conn.setReadTimeout(30000);     // 读取超时 30 秒（下载大文件需要更长时间）
            conn.setInstanceFollowRedirects(true);  // 自动跟随 HTTP 重定向
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; EasyPostman-Updater)");
            conn.setRequestProperty("Accept", "*/*");

            // 检查响应码
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            long totalSize = conn.getContentLengthLong();  // 使用 Long 版本支持大文件
            log.info("Starting download: {} bytes from {}", totalSize, downloadUrl);

            // 使用 try-with-resources 自动管理流资源
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 FileOutputStream out = new FileOutputStream(tempFile)) {

                long downloaded = performDownload(in, out, totalSize, callback);

                // 如果下载被取消
                if (cancelled) {
                    cleanupTempFile(tempFile);
                    callback.onCancelled();
                    return null;
                }

                log.info("Download completed: {} bytes", downloaded);
                callback.onCompleted(tempFile);
                return tempFile;
            }

        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    log.warn("Failed to disconnect connection: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 执行实际的下载操作
     */
    private long performDownload(InputStream in, FileOutputStream out, long totalSize,
                                  DownloadProgressCallback callback) throws IOException {
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

        return downloaded;
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile) {
        try {
            java.nio.file.Files.delete(tempFile.toPath());
        } catch (IOException e) {
            log.warn("Failed to delete temporary file: {}", e.getMessage());
        }
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
            String fileName = installerFile.getName().toLowerCase();

            // 检查是否为 JAR 文件
            if (fileName.endsWith(".jar")) {
                log.info("Installing JAR update: {}", installerFile.getAbsolutePath());
                JarUpdateManager jarUpdateManager = new JarUpdateManager();
                boolean success = jarUpdateManager.installJarUpdate(installerFile);
                callback.accept(success);
                // 注意：如果成功，JarUpdateManager 会自动重启应用并退出当前进程
                return;
            }

            // 其他文件类型（DMG、MSI、DEB、ZIP 等）使用系统默认方式打开
            log.info("Opening installer with default application: {}", installerFile.getAbsolutePath());
            Desktop.getDesktop().open(installerFile);
            callback.accept(true);
        } catch (Exception e) {
            log.error("Failed to open installer", e);
            callback.accept(false);
        }
    }
}
package com.laker.postman.service.http.okhttp;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OkHttp 响应处理工具类
 */
@Slf4j
public class OkHttpResponseHandler {

    private static int getMaxBodySize() {
        return SettingManager.getMaxBodySize();
    }

    private static int getMaxDownloadSize() {
        return SettingManager.getMaxDownloadSize();
    }

    public static void handleResponse(Response okResponse, HttpResponse response) throws IOException {
        response.code = okResponse.code();
        response.headers = new LinkedHashMap<>();
        int headersSize = 0;
        for (String name : okResponse.headers().names()) {
            String value = okResponse.header(name);
            if (value != null) {
                response.headers.put(name, List.of(value));
                headersSize += name.getBytes().length + 2; // key + ': '
                headersSize += value.getBytes().length + 2; // value + '\r\n'
            }
        }
        response.headersSize = headersSize;
        response.threadName = Thread.currentThread().getName();
        response.protocol = okResponse.protocol().toString();
        String contentType = okResponse.header("Content-Type", "");
        long contentLengthHeader = parseContentLength(okResponse.header("Content-Length"));

        if (isBinaryContent(contentType)) {
            handleBinaryResponse(okResponse, response);
        } else if (isSSEContent(contentType)) {
            response.body = "[SSE流响应，无法直接处理]";
            response.bodySize = 0;
            response.isSse = true;
            okResponse.body().close();
            okResponse.close();
        } else {
            handleTextResponse(okResponse, response, contentLengthHeader);
        }
    }

    private static long parseContentLength(String contentLengthStr) {
        if (contentLengthStr != null) {
            try {
                return Long.parseLong(contentLengthStr);
            } catch (NumberFormatException ignore) {
            }
        }
        return -1;
    }

    private static boolean isSSEContent(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains("text/event-stream");
    }


    private static boolean isBinaryContent(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        // 常见二进制类型判断
        return ct.contains("application/octet-stream")
                || ct.contains("application/pdf")
                || ct.contains("image/")
                || ct.contains("audio/")
                || ct.contains("video/")
                || ct.contains("application/zip")
                || ct.contains("application/x-zip-compressed")
                || ct.contains("application/msword")
                || ct.contains("application/vnd.openxml")
                || ct.contains("application/vnd.ms-excel")
                || ct.contains("application/vnd.ms-powerpoint")
                || ct.contains("application/x-tar")
                || ct.contains("application/gtar") // tar 变体
                || ct.contains("application/gzip")
                || ct.contains("application/x-7z-compressed")
                || ct.contains("application/x-rar-compressed")
                || ct.contains("application/vnd.android.package-archive")
                || ct.contains("application/x-bzip2") // bz2
                || ct.contains("application/x-xz") // xz
                || ct.contains("application/x-apple-diskimage") // dmg
                || ct.contains("application/x-iso9660-image") // iso
                || ct.contains("application/x-msdownload") // exe/dll
                || ct.contains("application/x-cpio") // cpio
                || ct.contains("application/x-debian-package") // deb
                || ct.contains("application/x-redhat-package-manager") // rpm
                || ct.contains("application/x-shockwave-flash") // swf
                // 以 application/x- 开头但不是常见文本类型的，视为二进制
                || (ct.startsWith("application/x-")
                && !ct.contains("x-www-form-urlencoded")
                && !ct.contains("x-javascript")
                && !ct.contains("x-json")
                && !ct.contains("x-shellscript")
                && !ct.contains("x-sh")
                && !ct.contains("x-python")
                && !ct.contains("x-java"));
    }

    /**
     * 保存输入流到临时文件，返回文件对象和写入的字节数
     * 优先从响应头获取 Content-Length
     */
    private static FileAndSize saveInputStreamToTempFile(InputStream is, String prefix, String suffix, long contentLengthHeader) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        final int[] totalBytes = {0};
        byte[] buf = new byte[64 * 1024];
        int len;
        long start = System.currentTimeMillis();
        long lastUpdate = start;
        final long[] contentLength = {contentLengthHeader};

        // 如果响应头没有，再从流获取
        if (contentLength[0] < 0 && is instanceof FileInputStream) {
            try {
                contentLength[0] = ((FileInputStream) is).getChannel().size();
            } catch (IOException ignored) {
            }
        }
        if (contentLength[0] < 0) {
            try {
                contentLength[0] = is.available();
            } catch (IOException ignored) {
            }
        }

        // 创建取消状态标志
        final boolean[] cancelled = new boolean[1];

        // 创建更美观的进度对话框
        JDialog progressDialog = new JDialog((JFrame) null, "Download Progress", true);
        progressDialog.setModal(false);
        progressDialog.setSize(350, 180);
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setResizable(false);

        // 使用BorderLayout布局
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // 添加 FlatSVGIcon 图标到标题左侧
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/download.svg", 24, 24));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.add(iconLabel);
        // 添加标题标签
        JLabel titleLabel = new JLabel("Downloading file...");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titlePanel.add(titleLabel);

        // 创建详细信息标签
        JLabel detailsLabel = new JLabel("Downloaded: 0 KB");
        JLabel speedLabel = new JLabel("Speed: 0 KB/s");
        JLabel timeLabel = new JLabel("Time left: Calculating...");

        // 放置标签的面板
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 0));
        infoPanel.add(detailsLabel);
        infoPanel.add(speedLabel);
        infoPanel.add(timeLabel);

        // 取消按钮
        JButton cancelButton = new JButton("Cancel", new FlatSVGIcon("icons/cancel.svg", 16, 16));
        cancelButton.addActionListener(e -> {
            cancelled[0] = true;
            progressDialog.dispose();
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);

        // 放置标签和按钮的面板（垂直排列）
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(infoPanel);
        southPanel.add(Box.createVerticalStrut(10));
        southPanel.add(buttonPanel);

        // 组装界面
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        // 不再使用 BorderLayout.EAST

        progressDialog.setContentPane(mainPanel);
        progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        progressDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelled[0] = true;
            }
        });

        // 只有大于5MB才显示进度对话框，否则后台下载不弹窗
        if (contentLength[0] > 5 * 1024 * 1024 || contentLength[0] <= 0) {
            progressDialog.setVisible(true);
        }


        // 创建更新UI的Runnable对象
        Runnable updateUI = () -> {
            if (!progressDialog.isDisplayable()) return;
            if (progressDialog.isVisible()) {
                long now = System.currentTimeMillis();
                long elapsed = now - start;
                double speed = elapsed > 0 ? (totalBytes[0] * 1000.0 / elapsed) : 0;
                String speedStr = speed > 1024 * 1024 ? String.format("%.2f MB/s", speed / (1024 * 1024)) : String.format("%.2f KB/s", speed / 1024);
                String sizeStr;
                if (totalBytes[0] > 1024 * 1024) {
                    if (contentLength[0] > 0) {
                        sizeStr = String.format("Downloaded: %.2f MB / %.2f MB", totalBytes[0] / (1024.0 * 1024), contentLength[0] / (1024.0 * 1024));
                    } else {
                        sizeStr = String.format("Downloaded: %.2f MB", totalBytes[0] / (1024.0 * 1024));
                    }
                } else {
                    if (contentLength[0] > 0) {
                        sizeStr = String.format("Downloaded: %.2f KB / %.2f KB", totalBytes[0] / 1024.0, contentLength[0] / 1024.0);
                    } else {
                        sizeStr = String.format("Downloaded: %.2f KB", totalBytes[0] / 1024.0);
                    }
                }
                String remainStr;
                if (contentLength[0] > 0 && speed > 0) {
                    long remainSeconds = (long) ((contentLength[0] - totalBytes[0]) / speed);
                    if (remainSeconds > 60) {
                        remainStr = String.format("Time left: %d min %d sec", remainSeconds / 60, remainSeconds % 60);
                    } else {
                        remainStr = String.format("Time left: %d sec", remainSeconds);
                    }
                } else {
                    remainStr = "Time left: Calculating...";
                }

                detailsLabel.setText(sizeStr);
                speedLabel.setText("Speed: " + speedStr);
                timeLabel.setText(remainStr);
            }
        };

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile), 64 * 1024)) {
            // 获取内容长度
            while ((len = is.read(buf)) != -1) {
                if (cancelled[0]) {
                    if (tempFile.exists()) tempFile.delete();
                    throw new IOException("下载已取消");
                }
                bos.write(buf, 0, len);
                totalBytes[0] += len;
                long now = System.currentTimeMillis();
                if (now - lastUpdate > 200) { // 每200毫秒更新一次UI
                    SwingUtilities.invokeLater(updateUI);
                    lastUpdate = now;
                }
            }
            SwingUtilities.invokeLater(updateUI);
        } catch (IOException e) {
            if (tempFile.exists()) tempFile.delete();
            throw e;
        } finally {
            SwingUtilities.invokeLater(progressDialog::dispose);
        }
        return new FileAndSize(tempFile, totalBytes[0]);
    }

    private static void handleBinaryResponse(Response okResponse, HttpResponse response) throws IOException {
        InputStream is = okResponse.body() != null ? okResponse.body().byteStream() : null;
        String fileName = null;
        // 优先 Content-Disposition
        String contentDisposition = getContentDisposition(response.headers);
        if (contentDisposition != null) {
            fileName = parseFileNameFromContentDisposition(contentDisposition);
        }
        // 其次 URL 路径
        if (fileName == null) {
            String url = okResponse.request().url().encodedPath();
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                String rawName = url.substring(lastSlash + 1);
                // 去除 !、?、# 及其后内容
                int excl = rawName.indexOf('!');
                int ques = rawName.indexOf('?');
                int sharp = rawName.indexOf('#');
                int cut = rawName.length();
                if (excl >= 0) cut = excl;
                if (ques >= 0 && ques < cut) cut = ques;
                if (sharp >= 0 && sharp < cut) cut = sharp;
                fileName = rawName.substring(0, cut);
            }
        }
        // 再次 Content-Type 猜扩展名
        if ((fileName == null || !fileName.contains(".")) && okResponse.header("Content-Type") != null) {
            String ext = guessExtensionFromContentType(okResponse.header("Content-Type"));
            if (fileName == null) fileName = "downloaded_file";
            if (ext != null && !fileName.endsWith(ext)) fileName += ext;
        }
        // 最后默认名
        if (fileName == null) fileName = "downloaded_file";
        response.fileName = fileName;
        int maxDownloadSize = getMaxDownloadSize();
        long contentLengthHeader = parseContentLength(okResponse.header("Content-Length"));
        if (maxDownloadSize > 0 && contentLengthHeader > maxDownloadSize) {
            if (is != null) is.close();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        String.format("二进制内容超出最大下载限制%dMB（当前限制：%d MB）", contentLengthHeader / 1024 / 1024, maxDownloadSize / 1024 / 1024),
                        "下载限制", JOptionPane.WARNING_MESSAGE);
            });
            response.body = String.format("[二进制内容超出最大下载限制，未下载。当前限制：%d MB]", maxDownloadSize / 1024 / 1024);
            response.bodySize = 0;
            response.filePath = null;
            return;
        }
        if (is != null) {
            FileAndSize fs = saveInputStreamToTempFile(is, "easyPostman_download_", null, contentLengthHeader);
            response.filePath = fs.file.getAbsolutePath();
            response.body = "[二进制内容，已保存为临时文件]";
            response.bodySize = fs.size;
        } else {
            response.body = "[无响应体]";
            response.bodySize = 0;
        }
    }

    private static String guessExtensionFromContentType(String contentType) {
        if (contentType == null) return null;
        String ct = contentType.toLowerCase();
        if (ct.contains("pdf")) return ".pdf";
        if (ct.contains("zip")) return ".zip";
        if (ct.contains("msword")) return ".doc";
        if (ct.contains("officedocument.wordprocessingml.document")) return ".docx";
        if (ct.contains("ms-excel")) return ".xls";
        if (ct.contains("officedocument.spreadsheetml.sheet")) return ".xlsx";
        if (ct.contains("ms-powerpoint")) return ".ppt";
        if (ct.contains("officedocument.presentationml.presentation")) return ".pptx";
        if (ct.contains("image/png")) return ".png";
        if (ct.contains("image/jpeg")) return ".jpg";
        if (ct.contains("image/gif")) return ".gif";
        if (ct.contains("image/bmp")) return ".bmp";
        if (ct.contains("image/webp")) return ".webp";
        if (ct.contains("audio/")) return ".mp3";
        if (ct.contains("video/")) return ".mp4";
        if (ct.contains("gzip")) return ".gz";
        if (ct.contains("x-tar")) return ".tar";
        if (ct.contains("x-7z-compressed")) return ".7z";
        if (ct.contains("x-rar-compressed")) return ".rar";
        if (ct.contains("apk")) return ".apk";
        return null;
    }

    private static void handleTextResponse(Response okResponse, HttpResponse response, long contentLengthHeader) throws IOException {
        String ext = guessExtensionFromContentType(okResponse.header("Content-Type"));
        int maxDownloadSize = getMaxDownloadSize();
        if (maxDownloadSize > 0 && contentLengthHeader > maxDownloadSize) {
            if (okResponse.body() != null) okResponse.body().close();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        String.format("文本内容超出最大下载限制%dMB（当前限制：%d MB）", contentLengthHeader / 1024 / 1024, maxDownloadSize / 1024 / 1024),
                        "下载限制", JOptionPane.WARNING_MESSAGE);
            });
            response.body = String.format("[文本内容超出最大下载限制，未下载。当前限制：%d MB]", maxDownloadSize / 1024 / 1024);
            response.bodySize = 0;
            response.filePath = null;
            return;
        }
        if (okResponse.body() != null) {
            String bodyStr = okResponse.body().string();
            if (bodyStr.getBytes().length > getMaxBodySize()) { // 如果响应体内容超过设置值，保存为临时文件
                // 这里也用流写入
                FileAndSize fs = saveInputStreamToTempFile(new ByteArrayInputStream(bodyStr.getBytes()), "easyPostman_text_download_", ext != null ? ext : ".txt", contentLengthHeader);
                response.filePath = fs.file.getAbsolutePath();
                response.fileName = "downloaded_text" + (ext != null ? ext : ".txt");
                int maxBodySizeKB = getMaxBodySize() / 1024;
                response.body = String.format("[响应体内容超过%dKB，已保存为临时文件，可下载查看完整内容]", maxBodySizeKB);
                response.bodySize = fs.size;
            } else {
                response.body = bodyStr;
                response.bodySize = bodyStr.getBytes().length;
                response.filePath = null;
            }
        } else {
            response.body = "";
            response.bodySize = 0;
            response.filePath = null;
        }
    }

    /**
     * 从响应头中提取Content-Disposition字段
     */
    public static String getContentDisposition(Map<String, List<String>> headers) {
        if (headers == null) return null;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && "Content-Disposition".equalsIgnoreCase(entry.getKey())) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    /**
     * 从Content-Disposition中解析文件名
     */
    public static String parseFileNameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null) return null;
        String lower = contentDisposition.toLowerCase();
        int idxStar = lower.indexOf("filename*=");
        if (idxStar >= 0) {
            String fn = contentDisposition.substring(idxStar + 9).trim();
            int firstQuote = fn.indexOf("''");
            if (firstQuote >= 0) {
                fn = fn.substring(firstQuote + 2);
            } else {
                int semi = fn.indexOf(';');
                if (semi > 0) fn = fn.substring(0, semi);
            }
            int semi = fn.indexOf(';');
            if (semi > 0) fn = fn.substring(0, semi);
            return fn.trim();
        }
        int idx = lower.indexOf("filename=");
        if (idx >= 0) {
            String fn = contentDisposition.substring(idx + 9).trim();
            if (fn.startsWith("\"")) fn = fn.substring(1);
            int end = fn.indexOf('"');
            if (end >= 0) fn = fn.substring(0, end);
            else {
                int semi = fn.indexOf(';');
                if (semi > 0) fn = fn.substring(0, semi);
            }
            return fn.trim();
        }
        return null;
    }

    // 文件和大小的简单封装
    private static class FileAndSize {
        File file;
        int size;

        FileAndSize(File file, int size) {
            this.file = file;
            this.size = size;
        }
    }
}

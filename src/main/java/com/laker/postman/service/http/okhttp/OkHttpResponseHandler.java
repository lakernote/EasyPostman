package com.laker.postman.service.http.okhttp;

import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

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

    public static HttpResponse handleResponse(Response okResponse, HttpResponse response) throws IOException {
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
        return response;
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
     */
    private static FileAndSize saveInputStreamToTempFile(InputStream is, String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        int totalBytes = 0;
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
                totalBytes += len;
            }
        }
        return new FileAndSize(tempFile, totalBytes);
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
        if (is != null) {
            FileAndSize fs = saveInputStreamToTempFile(is, "easyPostman_download_", null);
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
        if (contentLengthHeader > getMaxBodySize()) { // 如果 Content-Length 大于设置值，直接保存为临时文件
            InputStream is = okResponse.body() != null ? okResponse.body().byteStream() : null;
            if (is != null) {
                FileAndSize fs = saveInputStreamToTempFile(is, "easyPostman_text_download_", ext != null ? ext : ".txt");
                response.filePath = fs.file.getAbsolutePath();
                response.fileName = "downloaded_text" + (ext != null ? ext : ".txt");
                response.body = "[响应体内容超过10KB，已保存为临时文件，可下载查看完整内容]";
                response.bodySize = fs.size;
            } else {
                response.body = "[无响应体]";
                response.bodySize = 0;
                response.filePath = null;
            }
        } else if (okResponse.body() != null) {
            String bodyStr = okResponse.body().string();
            if (bodyStr.getBytes().length > getMaxBodySize()) { // 如果响应体内容超过设置值，保存为临时文件
                // 这里也用流写入
                FileAndSize fs = saveInputStreamToTempFile(new ByteArrayInputStream(bodyStr.getBytes()), "easyPostman_text_download_", ext != null ? ext : ".txt");
                response.filePath = fs.file.getAbsolutePath();
                response.fileName = "downloaded_text" + (ext != null ? ext : ".txt");
                response.body = "[响应体内容超过10KB，已保存为临时文件，可下载查看完整内容]";
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
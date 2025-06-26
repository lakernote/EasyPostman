package com.laker.postman.service.okhttp;

import com.laker.postman.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * OkHttp 响应处理工具类
 */
@Slf4j
public class OkHttpResponseHandler {
    private static final int MAX_BODY_SIZE = 10 * 1024; // 10KB

    public static HttpResponse handleResponse(Response okResponse, HttpResponse response) throws IOException {
        response.code = okResponse.code();
        response.headers = new HashMap<>();
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
        // 优先从响应头 Content-Length 获取 body 大小
        long contentLengthHeader = -1;
        String contentLengthStr = okResponse.header("Content-Length");
        if (contentLengthStr != null) {
            try {
                contentLengthHeader = Long.parseLong(contentLengthStr);
            } catch (NumberFormatException ignore) {
            }
        }
        // 处理文件下载和二进制内容
        if (contentType != null && (contentType.toLowerCase().contains("application/octet-stream")
                || contentType.toLowerCase().contains("application/pdf")
                || contentType.toLowerCase().contains("image/")
                || contentType.toLowerCase().contains("audio/")
                || contentType.toLowerCase().contains("video/"))) {
            InputStream is = okResponse.body() != null ? okResponse.body().byteStream() : null;
            if (is != null) {
                File tempFile = File.createTempFile("easyPostman_download_", null);
                int totalBytes = 0;
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        totalBytes += len;
                    }
                }
                response.filePath = tempFile.getAbsolutePath();
                response.body = "[二进制内容，已保存为临时文件]";
                response.bodySize = totalBytes;
            } else {
                response.body = "[无响应体]";
                response.bodySize = 0;
            }
        } else {
            // 处理文本内容
            if (contentLengthHeader > MAX_BODY_SIZE) {
                response.body = "[响应体内容超过10KB，未显示。请通过保存或下载查看完整内容]";
                response.bodySize = (int) contentLengthHeader;
            } else if (okResponse.body() != null) {
                String bodyStr = okResponse.body().string();
                if (bodyStr.getBytes().length > MAX_BODY_SIZE) {
                    response.body = "[响应体内容超过10KB，未显示。请通过保存或下载查看完整内容]";
                    response.bodySize = bodyStr.getBytes().length;
                } else {
                    response.body = bodyStr;
                    response.bodySize = bodyStr.getBytes().length;
                }
            } else {
                response.body = "";
                response.bodySize = 0;
            }
            response.filePath = null;
        }
        return response;
    }
}
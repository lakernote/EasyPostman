package com.laker.postman.util;

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
    public static HttpResponse handleResponse(Response okResponse, HttpResponse response) throws IOException {
        response.code = okResponse.code();
        response.headers = new HashMap<>();
        for (String name : okResponse.headers().names()) {
            String value = okResponse.header(name);
            if (value != null) {
                response.headers.put(name, List.of(value));
            }
        }
        response.threadName = Thread.currentThread().getName();
        response.protocol = okResponse.protocol().toString();
        String contentType = okResponse.header("Content-Type", "");
        if (contentType != null && (contentType.toLowerCase().contains("application/octet-stream")
                || contentType.toLowerCase().contains("application/pdf")
                || contentType.toLowerCase().contains("image/")
                || contentType.toLowerCase().contains("audio/")
                || contentType.toLowerCase().contains("video/"))) {
            InputStream is = okResponse.body() != null ? okResponse.body().byteStream() : null;
            if (is != null) {
                File tempFile = File.createTempFile("download_", null);
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                }
                response.filePath = tempFile.getAbsolutePath();
                response.body = "[二进制内容，已保存为临时文件]";
            } else {
                response.body = "[无响应体]";
            }
        } else {
            response.body = okResponse.body() != null ? okResponse.body().string() : "";
            response.filePath = null;
        }
        return response;
    }
}
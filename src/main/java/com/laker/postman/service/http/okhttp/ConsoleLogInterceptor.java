package com.laker.postman.service.http.okhttp;

import com.laker.postman.panel.SidebarTabPanel;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;

import java.io.IOException;

@Slf4j
public class ConsoleLogInterceptor implements Interceptor {
    private String headersToString(Headers headers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            String value = headers.value(i);
            if (name.equalsIgnoreCase("cookie")) {
                // 只保留可见字符，避免乱码
                value = value.replaceAll("[^\\x20-\\x7E]", "");
            }
            sb.append(name).append(": ").append(value).append("\n");
        }
        return sb.toString();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[HTTP Request] ").append("[").append(Thread.currentThread().getName()).append("] ")
                .append(request.method())
                .append(" ")
                .append(request.url())
                .append("\nHeaders: \n")
                .append(headersToString(request.headers()));
        if (request.body() != null) {
            MediaType contentType = request.body().contentType();
            if (contentType != null && (contentType.type().equalsIgnoreCase("multipart") || contentType.type().equalsIgnoreCase("application") && contentType.subtype().toLowerCase().contains("octet-stream"))) {
                logBuilder.append("Body: [Request body is file or binary, not logged]\n");
            } else {
                logBuilder.append("Body: ").append(bodyToString(request.body())).append("\n");
            }
        }
        SidebarTabPanel.appendConsoleLog(logBuilder.toString(), SidebarTabPanel.LogType.DEBUG);

        Response response = null;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            // 捕获异常并记录错误日志
            log.error("[HTTP Request] ", e);
            SidebarTabPanel.appendConsoleLog("[HTTP Error] " + e.getMessage(), SidebarTabPanel.LogType.ERROR);
            throw e;
        }
        StringBuilder respLog = new StringBuilder();
        respLog.append("[HTTP Response] ").append("[").append(Thread.currentThread().getName()).append("] ")
                .append(response.code())
                .append(" ")
                .append(response.message())
                .append("\nHeaders: \n")
                .append(headersToString(response.headers()));
        if (response.body() != null) {
            respLog.append("Body: [Response body not logged for security reasons]\n");
        }
        SidebarTabPanel.appendConsoleLog(respLog.toString(), SidebarTabPanel.LogType.SUCCESS);
        return response;
    }

    private String bodyToString(RequestBody body) {
        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            return buffer.readUtf8();
        } catch (Exception e) {
            return "[error reading body]";
        }
    }
}
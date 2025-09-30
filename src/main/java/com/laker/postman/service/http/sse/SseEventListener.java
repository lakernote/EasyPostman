package com.laker.postman.service.http.sse;

import com.laker.postman.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.LinkedHashMap;

// SSE事件监听器
@Slf4j
public class SseEventListener extends EventSourceListener {
    private final SseUiCallback callback;
    private final HttpResponse resp;
    private final StringBuilder sseBodyBuilder;
    private final long startTime;

    public SseEventListener(SseUiCallback callback, HttpResponse resp, StringBuilder sseBodyBuilder, long startTime) {
        this.callback = callback;
        this.resp = resp;
        this.sseBodyBuilder = sseBodyBuilder;
        this.startTime = startTime;
    }

    @Override
    public void onOpen(EventSource eventSource, okhttp3.Response response) {
        resp.headers = new LinkedHashMap<>();
        for (String name : response.headers().names()) {
            resp.addHeader(name, response.headers(name));
        }
        resp.code = response.code();
        resp.protocol = response.protocol().toString();
        callback.onOpen(resp, buildResponseHeadersTextStatic(resp));
    }

    @Override
    public void onClosed(EventSource eventSource) {
        long cost = System.currentTimeMillis() - startTime;
        resp.body = sseBodyBuilder.toString();
        resp.bodySize = resp.body.getBytes().length;
        resp.costMs = cost;
        callback.onClosed(resp);
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable throwable, okhttp3.Response response) {
        if (response != null) {
            log.error("sse onFailure,response status: {},response headers: {}", response.code(), response.headers(), throwable);
        } else {
            log.error("sse onFailure,response is null", throwable);
        }
        String errorMsg = throwable != null ? throwable.getMessage() : "未知错误";
        long cost = System.currentTimeMillis() - startTime;
        resp.body = sseBodyBuilder.toString();
        resp.bodySize = resp.body.getBytes().length;
        resp.costMs = cost;
        callback.onFailure(errorMsg, resp);
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        if (data != null && !data.isBlank()) {
            // 累积到 builder 用于最终的完整响应体
            sseBodyBuilder.append(data).append("\n");
            callback.onEvent(id, type, data);
        }
    }

    // 静态方法，避免内部类访问外部实例
    private static String buildResponseHeadersTextStatic(HttpResponse resp) {
        StringBuilder headersBuilder = new StringBuilder();
        resp.headers.forEach((key, value) -> {
            if (key != null) {
                headersBuilder.append(key).append(": ").append(String.join(", ", value)).append("\n");
            }
        });
        return headersBuilder.toString();
    }
}
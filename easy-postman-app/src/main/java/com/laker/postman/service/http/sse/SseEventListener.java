package com.laker.postman.service.http.sse;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.http.HttpService;
import com.laker.postman.service.http.NetworkErrorMessageResolver;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.function.BooleanSupplier;

// SSE事件监听器
@Slf4j
public class SseEventListener extends EventSourceListener {
    private final SseUiCallback callback;
    private final HttpResponse resp;
    private final StringBuilder sseBodyBuilder;
    private final long startTime;
    private final BooleanSupplier cancelledChecker;

    public SseEventListener(SseUiCallback callback, HttpResponse resp, StringBuilder sseBodyBuilder, long startTime,
                            BooleanSupplier cancelledChecker) {
        this.callback = callback;
        this.resp = resp;
        this.sseBodyBuilder = sseBodyBuilder;
        this.startTime = startTime;
        this.cancelledChecker = cancelledChecker;
    }

    @Override
    public void onOpen(EventSource eventSource, okhttp3.Response response) {
        resp.headers = new LinkedHashMap<>();
        for (String name : response.headers().names()) {
            resp.addHeader(name, response.headers(name));
        }
        resp.code = response.code();
        resp.protocol = response.protocol().toString();
        resp.isSse = true;
        HttpService.attachHttpEventInfo(resp, startTime);
        callback.onOpen(resp, buildResponseHeadersTextStatic(resp));
    }

    @Override
    public void onClosed(EventSource eventSource) {
        finalizeResponse();
        callback.onClosed(resp);
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable throwable, okhttp3.Response response) {
        boolean cancelled = cancelledChecker != null && cancelledChecker.getAsBoolean();
        if (cancelled) {
            if (response != null) {
                log.info("sse cancelled locally,response status: {},response headers: {}", response.code(), response.headers(), throwable);
            } else {
                log.info("sse cancelled locally,response is null", throwable);
            }
        } else if (response != null) {
            log.error("sse onFailure,response status: {},response headers: {}", response.code(), response.headers(), throwable);
        } else {
            log.error("sse onFailure,response is null", throwable);
        }
        if (response != null) {
            if (resp.headers == null) {
                resp.headers = new LinkedHashMap<>();
            }
            for (String name : response.headers().names()) {
                resp.addHeader(name, response.headers(name));
            }
            resp.code = response.code();
            resp.protocol = response.protocol().toString();
        }
        resp.isSse = true;
        if (resp.httpEventInfo == null) {
            HttpService.attachHttpEventInfo(resp, startTime);
        }
        finalizeResponse();
        if (cancelled) {
            callback.onClosed(resp);
            return;
        }
        String errorMsg = throwable != null
                ? NetworkErrorMessageResolver.toUserFriendlyMessage(throwable)
                : "未知错误";
        callback.onFailure(errorMsg, resp);
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        if (data != null && !data.isBlank()) {
            appendRawEvent(id, type, data);
            callback.onEvent(id, type, data);
        }
    }

    private void appendRawEvent(String id, String type, String data) {
        if (id != null && !id.isBlank()) {
            sseBodyBuilder.append("id: ").append(id).append('\n');
        }
        if (type != null && !type.isBlank()) {
            sseBodyBuilder.append("event: ").append(type).append('\n');
        }
        for (String line : data.split("\\R", -1)) {
            sseBodyBuilder.append("data: ").append(line).append('\n');
        }
        sseBodyBuilder.append('\n');
    }

    private void finalizeResponse() {
        long cost = System.currentTimeMillis() - startTime;
        resp.body = sseBodyBuilder.toString();
        resp.bodySize = resp.body.getBytes(StandardCharsets.UTF_8).length;
        resp.costMs = cost;
        resp.endTime = System.currentTimeMillis();
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

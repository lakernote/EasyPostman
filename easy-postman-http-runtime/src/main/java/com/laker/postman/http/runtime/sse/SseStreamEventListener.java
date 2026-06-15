package com.laker.postman.http.runtime.sse;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.error.NetworkErrorMessageResolver;
import com.laker.postman.http.runtime.transport.HttpExchangeTraceSupport;
import com.laker.postman.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.function.BooleanSupplier;

// SSE事件监听器
@Slf4j
public class SseStreamEventListener extends EventSourceListener {
    private static final int MAX_FAILURE_BODY_BYTES = 64 * 1024;

    private final SseStreamCallback callback;
    private final HttpResponse resp;
    private final StringBuilder sseBodyBuilder;
    private final long queueStartMs;
    private final BooleanSupplier cancelledChecker;
    private final PreparedRequest preparedRequest;

    public SseStreamEventListener(SseStreamCallback callback, HttpResponse resp, StringBuilder sseBodyBuilder, long queueStartMs,
                            BooleanSupplier cancelledChecker, PreparedRequest preparedRequest) {
        this.callback = callback;
        this.resp = resp;
        this.sseBodyBuilder = sseBodyBuilder;
        this.queueStartMs = queueStartMs;
        this.cancelledChecker = cancelledChecker;
        this.preparedRequest = preparedRequest;
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
        HttpExchangeTraceSupport.attachToResponse(resp, queueStartMs, preparedRequest);
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
            HttpExchangeTraceSupport.attachToResponse(resp, queueStartMs, preparedRequest);
        }
        String failureBody = captureFailureBody(response);
        boolean hasFailureBody = failureBody != null && !failureBody.isBlank();
        if (hasFailureBody) {
            resp.body = failureBody;
        }
        finalizeResponse(hasFailureBody);
        if (cancelled) {
            callback.onClosed(resp);
            return;
        }
        String errorMsg = resolveFailureMessage(throwable, response, failureBody);
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
        finalizeResponse(false);
    }

    private void finalizeResponse(boolean preserveExistingBody) {
        long cost = System.currentTimeMillis() - queueStartMs;
        if (!preserveExistingBody) {
            resp.body = sseBodyBuilder.toString();
        }
        if (resp.body == null) {
            resp.body = "";
        }
        resp.bodySize = resp.body.getBytes(StandardCharsets.UTF_8).length;
        resp.costMs = cost;
        resp.endTime = System.currentTimeMillis();
    }

    private String captureFailureBody(okhttp3.Response response) {
        if (response == null || response.body() == null) {
            return "";
        }
        ResponseBody body = response.body();
        Charset charset = resolveCharset(body);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(MAX_FAILURE_BODY_BYTES, 8 * 1024));
        byte[] buffer = new byte[8 * 1024];
        int total = 0;
        boolean truncated = false;
        try (InputStream inputStream = body.byteStream()) {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                int remaining = MAX_FAILURE_BODY_BYTES - total;
                if (remaining <= 0) {
                    truncated = true;
                    break;
                }
                int bytesToKeep = Math.min(len, remaining);
                out.write(buffer, 0, bytesToKeep);
                total += bytesToKeep;
                if (bytesToKeep < len) {
                    truncated = true;
                    break;
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to read SSE failure response body: {}", ex.getMessage());
            return "";
        }
        String text = new String(out.toByteArray(), charset);
        if (truncated) {
            return text + "\n\n[Truncated SSE failure body: showing first " + (MAX_FAILURE_BODY_BYTES / 1024) + "KB]";
        }
        return text;
    }

    private Charset resolveCharset(ResponseBody body) {
        MediaType mediaType = body.contentType();
        if (mediaType != null) {
            Charset charset = mediaType.charset();
            if (charset != null) {
                return charset;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String resolveFailureMessage(Throwable throwable, okhttp3.Response response, String failureBody) {
        String detail = extractFailureDetail(failureBody);
        String status = response == null ? "" : "HTTP " + response.code() + responseMessage(response);
        if (detail != null && !detail.isBlank()) {
            return status.isBlank() ? detail : status + ": " + detail;
        }
        if (throwable != null) {
            String message = NetworkErrorMessageResolver.toUserFriendlyMessage(throwable);
            if (message != null && !message.isBlank()) {
                return status.isBlank() ? message : status + ": " + message;
            }
        }
        if (!status.isBlank()) {
            return status;
        }
        return "未知错误";
    }

    private String responseMessage(okhttp3.Response response) {
        String message = response.message();
        if (message == null || message.isBlank()) {
            return "";
        }
        return " " + message;
    }

    private String extractFailureDetail(String failureBody) {
        if (failureBody == null || failureBody.isBlank()) {
            return null;
        }
        String trimmed = failureBody.trim();
        try {
            JsonNode node = JsonUtil.readTree(trimmed);
            if (node != null && node.isObject()) {
                for (String key : new String[]{"detail", "error", "message", "error_description"}) {
                    JsonNode value = node.get(key);
                    if (value != null && value.isTextual() && !value.asText().isBlank()) {
                        return value.asText();
                    }
                }
            }
        } catch (Exception ignored) {
            // 非 JSON 错误体直接使用原文前缀。
        }
        return trimmed.length() > 2048 ? trimmed.substring(0, 2048) + "..." : trimmed;
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

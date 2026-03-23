package com.laker.postman.plugin.capture;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

final class CaptureFlow {
    private static final AtomicLong IDS = new AtomicLong(1);
    private static final int PREVIEW_LIMIT = 64 * 1024;

    private final String id;
    private final long startedAt;
    private final String method;
    private final String url;
    private final String host;
    private final String path;
    private final Map<String, String> requestHeaders;
    private final byte[] requestBody;
    private final int requestSize;

    private volatile long completedAt;
    private volatile int statusCode;
    private volatile String statusText = "";
    private volatile String errorMessage = "";
    private volatile Map<String, String> responseHeaders = Map.of();
    private volatile byte[] responseBody = new byte[0];
    private volatile int responseSize;

    CaptureFlow(String method,
                String url,
                String host,
                String path,
                Map<String, String> requestHeaders,
                byte[] requestBody) {
        this.id = String.valueOf(IDS.getAndIncrement());
        this.startedAt = System.currentTimeMillis();
        this.method = method;
        this.url = url;
        this.host = host;
        this.path = path;
        this.requestHeaders = new LinkedHashMap<>(requestHeaders);
        this.requestSize = requestBody == null ? 0 : requestBody.length;
        this.requestBody = trimPreview(requestBody);
    }

    String id() {
        return id;
    }

    String timeText() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(startedAt));
    }

    String method() {
        return method;
    }

    String host() {
        return host;
    }

    String path() {
        return path;
    }

    int statusCode() {
        return statusCode;
    }

    String statusText() {
        return statusText;
    }

    long durationMs() {
        long finished = completedAt > 0 ? completedAt : System.currentTimeMillis();
        return Math.max(0, finished - startedAt);
    }

    int requestSize() {
        return requestSize;
    }

    int responseSize() {
        return responseSize;
    }

    String requestBodyPreview() {
        return toPreviewText(requestBody);
    }

    String responseBodyPreview() {
        return toPreviewText(responseBody);
    }

    void complete(int statusCode, String statusText, Map<String, String> responseHeaders, byte[] responseBody) {
        this.statusCode = statusCode;
        this.statusText = statusText == null ? "" : statusText;
        this.responseHeaders = new LinkedHashMap<>(responseHeaders);
        this.responseSize = responseBody == null ? 0 : responseBody.length;
        this.responseBody = trimPreview(responseBody);
        this.completedAt = System.currentTimeMillis();
    }

    void fail(int statusCode, String errorMessage) {
        this.statusCode = statusCode;
        this.statusText = statusCode > 0 ? "ERROR" : "";
        this.errorMessage = errorMessage == null ? "" : errorMessage;
        this.completedAt = System.currentTimeMillis();
    }

    Object[] toRow() {
        return new Object[]{
                id,
                timeText(),
                method,
                host,
                path,
                statusCode > 0 ? statusCode : "",
                durationMs(),
                requestSize(),
                responseSize()
        };
    }

    String detailText() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "ID", id);
        appendLine(builder, "Time", new Date(startedAt).toString());
        appendLine(builder, "Method", method);
        appendLine(builder, "URL", url);
        appendLine(builder, "Status", statusCode > 0 ? statusCode + " " + statusText : "PENDING");
        appendLine(builder, "Duration", durationMs() + " ms");
        if (!errorMessage.isBlank()) {
            appendLine(builder, "Error", errorMessage);
        }

        builder.append("\nRequest Headers\n");
        builder.append("---------------\n");
        appendHeaders(builder, requestHeaders);

        builder.append("\nRequest Body\n");
        builder.append("------------\n");
        builder.append(requestBodyPreview()).append('\n');

        builder.append("\nResponse Headers\n");
        builder.append("----------------\n");
        appendHeaders(builder, responseHeaders);

        builder.append("\nResponse Body\n");
        builder.append("-------------\n");
        builder.append(responseBodyPreview()).append('\n');
        return builder.toString();
    }

    private static void appendLine(StringBuilder builder, String key, String value) {
        builder.append(key).append(": ").append(value == null ? "" : value).append('\n');
    }

    private static void appendHeaders(StringBuilder builder, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            builder.append("(empty)\n");
            return;
        }
        headers.forEach((name, value) -> builder.append(name).append(": ").append(value).append('\n'));
    }

    private static byte[] trimPreview(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        if (bytes.length <= PREVIEW_LIMIT) {
            return bytes;
        }
        byte[] preview = new byte[PREVIEW_LIMIT];
        System.arraycopy(bytes, 0, preview, 0, PREVIEW_LIMIT);
        return preview;
    }

    private static String toPreviewText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "(empty)";
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (looksPrintable(text)) {
            return text;
        }
        StringBuilder hex = new StringBuilder();
        int limit = Math.min(bytes.length, 256);
        for (int i = 0; i < limit; i++) {
            if (i > 0 && i % 16 == 0) {
                hex.append('\n');
            } else if (i > 0) {
                hex.append(' ');
            }
            hex.append(String.format("%02X", bytes[i]));
        }
        if (bytes.length > limit) {
            hex.append("\n... truncated ...");
        }
        return hex.toString();
    }

    private static boolean looksPrintable(String text) {
        int printable = 0;
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            total++;
            if (Character.isWhitespace(c) || !Character.isISOControl(c)) {
                printable++;
            }
        }
        return total == 0 || printable * 100 / total >= 90;
    }
}

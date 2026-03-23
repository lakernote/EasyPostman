package com.laker.postman.plugin.capture;

import com.laker.postman.util.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

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

    String url() {
        return url;
    }

    int statusCode() {
        return statusCode;
    }

    String statusText() {
        return statusText;
    }

    String startedAtText() {
        return new Date(startedAt).toString();
    }

    String statusDisplayText() {
        return statusCode > 0 ? statusCode + " " + statusText : t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PENDING);
    }

    int requestHeaderCount() {
        return requestHeaders.size();
    }

    int responseHeaderCount() {
        return responseHeaders.size();
    }

    String requestContentType() {
        return headerValueIgnoreCase(requestHeaders, "Content-Type");
    }

    String responseContentType() {
        return headerValueIgnoreCase(responseHeaders, "Content-Type");
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
        this.statusText = statusCode > 0 ? t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_ERROR_STATUS) : "";
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
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_ID), id);
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_TIME), new Date(startedAt).toString());
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD), method);
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_URL), url);
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS),
                statusCode > 0 ? statusCode + " " + statusText : t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PENDING));
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION), durationMs() + " ms");
        if (!errorMessage.isBlank()) {
            appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_ERROR), errorMessage);
        }

        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_REQUEST_HEADERS)).append('\n');
        builder.append("---------------\n");
        appendHeaders(builder, requestHeaders);

        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_REQUEST_BODY)).append('\n');
        builder.append("------------\n");
        builder.append(requestBodyPreview()).append('\n');

        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_HEADERS)).append('\n');
        builder.append("----------------\n");
        appendHeaders(builder, responseHeaders);

        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_BODY)).append('\n');
        builder.append("-------------\n");
        builder.append(responseBodyPreview()).append('\n');
        return builder.toString();
    }

    String requestDetailText() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD), method);
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_URL), url);
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_TIME), new Date(startedAt).toString());
        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_REQUEST_HEADERS)).append('\n');
        builder.append("---------------\n");
        appendHeaders(builder, requestHeaders);
        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_REQUEST_BODY)).append('\n');
        builder.append("------------\n");
        builder.append(requestBodyPreview()).append('\n');
        return builder.toString();
    }

    String responseDetailText() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS), statusDisplayText());
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION), durationMs() + " ms");
        if (!errorMessage.isBlank()) {
            appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_ERROR), errorMessage);
        }
        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_HEADERS)).append('\n');
        builder.append("----------------\n");
        appendHeaders(builder, responseHeaders);
        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_BODY)).append('\n');
        builder.append("-------------\n");
        builder.append(responseBodyPreview()).append('\n');
        return builder.toString();
    }

    String curlCommand() {
        StringBuilder builder = new StringBuilder("curl");
        appendCurlArgument(builder, "-X");
        appendCurlArgument(builder, method);
        appendCurlArgument(builder, url);
        requestHeaders.forEach((name, value) -> {
            if (shouldIncludeCurlHeader(name)) {
                appendCurlArgument(builder, "-H");
                appendCurlArgument(builder, name + ": " + value);
            }
        });
        String bodyText = curlBodyText();
        if (bodyText != null) {
            appendCurlArgument(builder, "--data-raw");
            appendCurlArgument(builder, bodyText);
        }
        return builder.toString();
    }

    boolean curlBodyPartial() {
        if (requestSize == 0) {
            return false;
        }
        String bodyText = curlBodyText();
        return bodyText == null || requestSize > requestBody.length;
    }

    private static void appendLine(StringBuilder builder, String key, String value) {
        builder.append(key).append(": ").append(value == null ? "" : value).append('\n');
    }

    private static void appendHeaders(StringBuilder builder, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            builder.append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_EMPTY)).append('\n');
            return;
        }
        headers.forEach((name, value) -> builder.append(name).append(": ").append(value).append('\n'));
    }

    private String curlBodyText() {
        if (requestBody == null || requestBody.length == 0) {
            return null;
        }
        String text = new String(requestBody, StandardCharsets.UTF_8);
        return looksPrintable(text) ? text : null;
    }

    private static boolean shouldIncludeCurlHeader(String name) {
        return !"Content-Length".equalsIgnoreCase(name)
                && !"Host".equalsIgnoreCase(name)
                && !"Proxy-Connection".equalsIgnoreCase(name);
    }

    private static void appendCurlArgument(StringBuilder builder, String argument) {
        builder.append(" \\\n  ").append(shellQuote(argument));
    }

    private static String shellQuote(String value) {
        return "'" + (value == null ? "" : value.replace("'", "'\"'\"'")) + "'";
    }

    private static String headerValueIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() == null ? "" : entry.getValue();
            }
        }
        return "";
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
            if (JsonUtil.isTypeJSON(text)) {
                String pretty = JsonUtil.toJsonPrettyStr(text);
                return pretty == null ? text : pretty;
            }
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
            hex.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_TRUNCATED));
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

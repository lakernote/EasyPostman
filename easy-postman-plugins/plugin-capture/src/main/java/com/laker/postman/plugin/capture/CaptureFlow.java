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
    private static final int TEXT_PREVIEW_LIMIT = 64 * 1024;
    private static final String STREAM_SEPARATOR = "\n\n";

    private final String id;
    private final long startedAt;
    private final String method;
    private final String url;
    private final String host;
    private final String path;
    private final Map<String, String> requestHeaders;
    private volatile byte[] requestBody;
    private volatile int requestSize;

    private volatile long completedAt;
    private volatile int statusCode;
    private volatile String statusText = "";
    private volatile String errorMessage = "";
    private volatile Map<String, String> responseHeaders = Map.of();
    private volatile byte[] responseBody = new byte[0];
    private volatile int responseSize;
    private volatile Protocol protocol;
    private volatile String requestStreamPreview = "";
    private volatile String responseStreamPreview = "";
    private volatile String streamTimelinePreview = "";
    private volatile int streamEventCount;

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
        this.protocol = detectInitialProtocol(requestHeaders);
    }

    String id() {
        return id;
    }

    int sequence() {
        return Integer.parseInt(id);
    }

    String timeText() {
        return formatTime(startedAt);
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

    String collectionRequestUrl() {
        if (protocol != Protocol.WEBSOCKET) {
            return url;
        }
        if (url.startsWith("https://")) {
            return "wss://" + url.substring("https://".length());
        }
        if (url.startsWith("http://")) {
            return "ws://" + url.substring("http://".length());
        }
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

    String protocolText() {
        return switch (protocol) {
            case HTTP -> t(MessageKeys.TOOLBOX_CAPTURE_PROTOCOL_HTTP);
            case SSE -> t(MessageKeys.TOOLBOX_CAPTURE_PROTOCOL_SSE);
            case WEBSOCKET -> t(MessageKeys.TOOLBOX_CAPTURE_PROTOCOL_WEBSOCKET);
        };
    }

    boolean isSseProtocol() {
        return protocol == Protocol.SSE;
    }

    boolean isWebSocketProtocol() {
        return protocol == Protocol.WEBSOCKET;
    }

    String statusDisplayText() {
        return statusCode > 0 ? statusCode + " " + statusText : t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PENDING);
    }

    int streamEventCount() {
        return streamEventCount;
    }

    int requestHeaderCount() {
        return requestHeaders.size();
    }

    Map<String, String> requestHeadersSnapshot() {
        return new LinkedHashMap<>(requestHeaders);
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
        if (!requestStreamPreview.isBlank()) {
            return requestStreamPreview;
        }
        return toPreviewText(requestBody);
    }

    String responseBodyPreview() {
        if (!responseStreamPreview.isBlank()) {
            return responseStreamPreview;
        }
        return toPreviewText(responseBody);
    }

    void recordResponseStart(int statusCode, String statusText, Map<String, String> responseHeaders) {
        this.statusCode = statusCode;
        this.statusText = statusText == null ? "" : statusText;
        this.responseHeaders = new LinkedHashMap<>(responseHeaders);
        protocol = detectResponseProtocol(protocol, statusCode, responseHeaders);
    }

    synchronized void appendRequestBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        requestSize += bytes.length;
        requestBody = appendPreview(requestBody, bytes);
    }

    synchronized void appendResponseBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        responseSize += bytes.length;
        responseBody = appendPreview(responseBody, bytes);
        if (protocol == Protocol.SSE) {
            appendResponseStreamEvent(formatStreamChunkEvent("SSE", bytes));
        }
    }

    synchronized void appendRequestStreamEvent(String text) {
        requestStreamPreview = appendTextPreview(requestStreamPreview, text);
        appendTimelineEvent("CLIENT", text);
    }

    synchronized void appendResponseStreamEvent(String text) {
        responseStreamPreview = appendTextPreview(responseStreamPreview, text);
        appendTimelineEvent(protocol == Protocol.SSE ? "SSE" : "SERVER", text);
    }

    void complete() {
        this.completedAt = System.currentTimeMillis();
    }

    void complete(int statusCode, String statusText, Map<String, String> responseHeaders, byte[] responseBody) {
        recordResponseStart(statusCode, statusText, responseHeaders);
        this.responseSize = 0;
        this.responseBody = new byte[0];
        appendResponseBody(responseBody);
        complete();
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
                sequence(),
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
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL), protocolText());
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
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL), protocolText());
        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_REQUEST_HEADERS)).append('\n');
        builder.append("---------------\n");
        appendHeaders(builder, requestHeaders);
        builder.append('\n').append(requestBodySectionTitle()).append('\n');
        builder.append("------------\n");
        builder.append(requestBodyPreview()).append('\n');
        return builder.toString();
    }

    String responseDetailText() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL), protocolText());
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS), statusDisplayText());
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION), durationMs() + " ms");
        if (!errorMessage.isBlank()) {
            appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_ERROR), errorMessage);
        }
        builder.append('\n').append(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_HEADERS)).append('\n');
        builder.append("----------------\n");
        appendHeaders(builder, responseHeaders);
        builder.append('\n').append(responseBodySectionTitle()).append('\n');
        builder.append("-------------\n");
        builder.append(responseBodyPreview()).append('\n');
        return builder.toString();
    }

    String streamDetailText() {
        if (!streamTimelinePreview.isBlank()) {
            return streamTimelinePreview;
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_NO_STREAM);
    }

    String requestBodyImportText() {
        if (protocol == Protocol.WEBSOCKET || requestBody == null || requestBody.length == 0) {
            return "";
        }
        String text = new String(requestBody, StandardCharsets.UTF_8);
        return looksPrintable(text) ? text : "";
    }

    boolean requestBodyPartial() {
        return requestSize > (requestBody == null ? 0 : requestBody.length);
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
        if (protocol == Protocol.WEBSOCKET) {
            return true;
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
        if (protocol == Protocol.WEBSOCKET) {
            return null;
        }
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

    private static byte[] appendPreview(byte[] existingPreview, byte[] appendedBytes) {
        if (appendedBytes == null || appendedBytes.length == 0) {
            return existingPreview == null ? new byte[0] : existingPreview;
        }
        byte[] current = existingPreview == null ? new byte[0] : existingPreview;
        if (current.length >= PREVIEW_LIMIT) {
            return current;
        }
        int appendLength = Math.min(appendedBytes.length, PREVIEW_LIMIT - current.length);
        byte[] merged = new byte[current.length + appendLength];
        System.arraycopy(current, 0, merged, 0, current.length);
        System.arraycopy(appendedBytes, 0, merged, current.length, appendLength);
        return merged;
    }

    private static String appendTextPreview(String existingPreview, String appendedText) {
        String current = existingPreview == null ? "" : existingPreview;
        if (appendedText == null || appendedText.isBlank() || current.length() >= TEXT_PREVIEW_LIMIT) {
            return current;
        }
        String normalized = current.isEmpty() ? appendedText : current + STREAM_SEPARATOR + appendedText;
        if (normalized.length() <= TEXT_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, TEXT_PREVIEW_LIMIT) + STREAM_SEPARATOR + t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_TRUNCATED);
    }

    private synchronized void appendTimelineEvent(String direction, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        streamEventCount++;
        String entry = "[" + formatTime(System.currentTimeMillis()) + "] " + direction + "\n" + text;
        streamTimelinePreview = appendTextPreview(streamTimelinePreview, entry);
    }

    private static String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
    }

    private static Protocol detectInitialProtocol(Map<String, String> requestHeaders) {
        String upgrade = headerValueIgnoreCase(requestHeaders, "Upgrade");
        if ("websocket".equalsIgnoreCase(upgrade)) {
            return Protocol.WEBSOCKET;
        }
        String accept = headerValueIgnoreCase(requestHeaders, "Accept");
        if (accept.toLowerCase().contains("text/event-stream")) {
            return Protocol.SSE;
        }
        return Protocol.HTTP;
    }

    private static Protocol detectResponseProtocol(Protocol currentProtocol, int statusCode, Map<String, String> responseHeaders) {
        String upgrade = headerValueIgnoreCase(responseHeaders, "Upgrade");
        if (statusCode == 101 && "websocket".equalsIgnoreCase(upgrade)) {
            return Protocol.WEBSOCKET;
        }
        String contentType = headerValueIgnoreCase(responseHeaders, "Content-Type").toLowerCase();
        if (contentType.contains("text/event-stream")) {
            return Protocol.SSE;
        }
        return currentProtocol == null ? Protocol.HTTP : currentProtocol;
    }

    private String requestBodySectionTitle() {
        return protocol == Protocol.WEBSOCKET
                ? t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_REQUEST_STREAM)
                : t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_REQUEST_BODY);
    }

    private String responseBodySectionTitle() {
        return protocol == Protocol.WEBSOCKET
                ? t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_STREAM)
                : t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_BODY);
    }

    private String formatStreamChunkEvent(String prefix, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return prefix;
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        String preview = looksPrintable(text) ? text : toPreviewText(bytes);
        return prefix + " chunk len=" + bytes.length + "\n" + preview;
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

    private enum Protocol {
        HTTP,
        SSE,
        WEBSOCKET
    }
}

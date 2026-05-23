package com.laker.postman.panel.performance.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PerformanceInternalHeaders {
    private static final String WS_PREFIX = "x-easy-ws-";
    private static final String SSE_PREFIX = "x-easy-sse-";
    private static final String WS_ERROR = "X-Easy-WS-Error";
    private static final String SSE_ERROR = "X-Easy-SSE-Error";

    private PerformanceInternalHeaders() {
    }

    public static boolean isInternalHeader(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.startsWith(WS_PREFIX) || normalized.startsWith(SSE_PREFIX);
    }

    public static void removeInternalHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.entrySet().removeIf(entry -> isInternalHeader(entry.getKey()));
    }

    public static String firstStreamError(Map<String, List<String>> headers) {
        String webSocketError = firstHeaderValue(headers, WS_ERROR);
        if (webSocketError != null && !webSocketError.isBlank()) {
            return webSocketError;
        }
        String sseError = firstHeaderValue(headers, SSE_ERROR);
        return sseError == null ? "" : sseError;
    }

    private static String firstHeaderValue(Map<String, List<String>> headers, String name) {
        if (headers == null || name == null) {
            return "";
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!name.equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty() || values.get(0) == null) {
                return "";
            }
            return values.get(0);
        }
        return "";
    }
}

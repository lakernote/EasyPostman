package com.laker.postman.plugin.capture;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Map;

@UtilityClass
class CaptureFlowClassifier {

    private static final String[] STATIC_EXTENSIONS = {
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico", ".bmp", ".avif",
            ".css", ".js", ".mjs", ".cjs",
            ".woff", ".woff2", ".ttf", ".otf", ".eot",
            ".mp4", ".mp3", ".wav", ".webm", ".ogg", ".m4a", ".mov", ".avi", ".mkv"
    };
    private static final String[] TELEMETRY_HOST_TERMS = {
            "analytics", "telemetry", "collector", "clarity", "nr-data", "sentry", "datadog", "launchdarkly"
    };
    private static final String[] TELEMETRY_PATH_TERMS = {
            "/collect", "/telemetry", "/analytics", "/events", "/logs", "/rum", "/beacon", "/metrics"
    };

    static boolean isError(CaptureFlow flow) {
        return flow != null && switch (CaptureStatusStyle.toneFor(flow.statusCode())) {
            case CLIENT_ERROR, FAILURE -> true;
            default -> false;
        };
    }

    static boolean isSlow(CaptureFlow flow) {
        return flow != null && CaptureTableStyle.isSlow(flow.durationMs());
    }

    static boolean isStaticResource(CaptureFlow flow) {
        if (flow == null) {
            return false;
        }
        String path = lower(flow.path());
        if (hasExtension(path, STATIC_EXTENSIONS)) {
            return true;
        }
        String contentType = lower(firstNonBlank(flow.responseContentType(), flow.requestContentType()));
        return contentType.startsWith("image/")
                || contentType.contains("javascript")
                || contentType.contains("text/css")
                || contentType.startsWith("font/")
                || contentType.startsWith("audio/")
                || contentType.startsWith("video/");
    }

    static boolean isTelemetry(CaptureFlow flow) {
        if (flow == null) {
            return false;
        }
        String host = lower(flow.host());
        String path = lower(flow.path());
        if (containsAny(host, TELEMETRY_HOST_TERMS)) {
            return true;
        }
        return containsAny(path, TELEMETRY_PATH_TERMS);
    }

    static boolean isApiTraffic(CaptureFlow flow) {
        if (flow == null || isStaticResource(flow)) {
            return false;
        }
        String path = lower(flow.path());
        String url = lower(flow.url());
        if (path.startsWith("/api") || path.contains("/api/")
                || path.startsWith("/backend-api") || path.contains("/backend-api/")
                || url.contains("/graphql")) {
            return true;
        }
        Map<String, String> requestHeaders = flow.requestHeadersSnapshot();
        String accept = lower(header(requestHeaders, "Accept"));
        String contentType = lower(firstNonBlank(flow.requestContentType(), flow.responseContentType()));
        return accept.contains("application/json")
                || accept.contains("text/event-stream")
                || contentType.contains("application/json")
                || contentType.contains("text/event-stream");
    }

    static String resourceType(CaptureFlow flow) {
        if (flow == null) {
            return "other";
        }
        Map<String, String> requestHeaders = flow.requestHeadersSnapshot();
        String path = lower(flow.path());
        String accept = lower(header(requestHeaders, "Accept"));
        String contentType = lower(firstNonBlank(flow.requestContentType(), flow.responseContentType()));
        String fetchDest = lower(header(requestHeaders, "Sec-Fetch-Dest"));
        String upgrade = lower(header(requestHeaders, "Upgrade"));
        String requestedWith = lower(header(requestHeaders, "X-Requested-With"));

        if ("websocket".equals(upgrade) || flow.isWebSocketProtocol()) {
            return "websocket";
        }
        if (accept.contains("text/event-stream") || contentType.contains("text/event-stream") || flow.isSseProtocol()) {
            return "sse";
        }
        if ("image".equals(fetchDest) || contentType.startsWith("image/")
                || hasExtension(path, ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico", ".bmp", ".avif", ".tif", ".tiff")) {
            return "image";
        }
        if ("style".equals(fetchDest) || contentType.contains("text/css") || hasExtension(path, ".css")) {
            return "css";
        }
        if ("script".equals(fetchDest) || contentType.contains("javascript") || hasExtension(path, ".js", ".mjs", ".cjs")) {
            return "js";
        }
        if ("font".equals(fetchDest) || contentType.startsWith("font/")
                || hasExtension(path, ".woff", ".woff2", ".ttf", ".otf", ".eot")) {
            return "font";
        }
        if ("audio".equals(fetchDest) || "video".equals(fetchDest)
                || contentType.startsWith("audio/") || contentType.startsWith("video/")
                || hasExtension(path, ".mp4", ".mp3", ".wav", ".webm", ".ogg", ".m4a", ".mov", ".avi", ".mkv")) {
            return "media";
        }
        if (contentType.contains("application/json") || accept.contains("application/json") || hasExtension(path, ".json")) {
            return "json";
        }
        if ("xmlhttprequest".equals(requestedWith) || isApiTraffic(flow)) {
            return "api";
        }
        if ("document".equals(fetchDest) || "iframe".equals(fetchDest) || "frame".equals(fetchDest)
                || contentType.contains("text/html") || hasExtension(path, ".html", ".htm")) {
            return "html";
        }
        return "other";
    }

    static int errorPriority(CaptureFlow flow) {
        if (flow == null) {
            return 50;
        }
        int statusCode = flow.statusCode();
        if (statusCode == 495 || statusCode >= 500) {
            return 0;
        }
        if (statusCode == 429) {
            return 1;
        }
        if (statusCode >= 400) {
            return 2;
        }
        if (isSlow(flow)) {
            return 3;
        }
        if (statusCode >= 300) {
            return 4;
        }
        return 50;
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null || name == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static boolean hasExtension(String path, String... extensions) {
        String normalizedPath = path == null ? "" : path;
        int queryIndex = normalizedPath.indexOf('?');
        if (queryIndex >= 0) {
            normalizedPath = normalizedPath.substring(0, queryIndex);
        }
        for (String extension : extensions) {
            if (normalizedPath.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

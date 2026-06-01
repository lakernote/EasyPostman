package com.laker.postman.request.defaults;

import com.laker.postman.request.model.HttpHeader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GeneratedRequestHeaderPolicy {
    private final List<HttpHeader> generatedHeaders;

    private GeneratedRequestHeaderPolicy(List<HttpHeader> generatedHeaders) {
        this.generatedHeaders = copyHeaders(generatedHeaders);
    }

    public static GeneratedRequestHeaderPolicy standard(String userAgentValue) {
        return new GeneratedRequestHeaderPolicy(HttpRequestDefaults.standardHttpHeaders(userAgentValue));
    }

    public List<HttpHeader> generatedHeaders() {
        return copyHeaders(generatedHeaders);
    }

    public int generatedHeaderCount() {
        return generatedHeaders.size();
    }

    public boolean isGeneratedHeaderKey(String key) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey.isEmpty()) {
            return false;
        }
        for (HttpHeader header : generatedHeaders) {
            if (normalizeKey(header.getKey()).equals(normalizedKey)) {
                return true;
            }
        }
        return false;
    }

    public List<HttpHeader> applyDefaults(List<HttpHeader> userHeaders) {
        Map<String, HttpHeader> userHeadersByKey = new LinkedHashMap<>();
        if (userHeaders != null) {
            for (HttpHeader header : userHeaders) {
                if (header == null || normalizeKey(header.getKey()).isEmpty()) {
                    continue;
                }
                userHeadersByKey.putIfAbsent(normalizeKey(header.getKey()), copyHeader(header));
            }
        }

        List<HttpHeader> merged = new ArrayList<>();
        for (HttpHeader generatedHeader : generatedHeaders) {
            HttpHeader userHeader = userHeadersByKey.get(normalizeKey(generatedHeader.getKey()));
            merged.add(userHeader != null ? userHeader : copyHeader(generatedHeader));
        }

        if (userHeaders != null) {
            for (HttpHeader header : userHeaders) {
                if (header == null || normalizeKey(header.getKey()).isEmpty()) {
                    continue;
                }
                if (!isGeneratedHeaderKey(header.getKey())) {
                    merged.add(copyHeader(header));
                }
            }
        }
        return merged;
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private static List<HttpHeader> copyHeaders(List<HttpHeader> headers) {
        List<HttpHeader> copied = new ArrayList<>();
        if (headers == null) {
            return copied;
        }
        for (HttpHeader header : headers) {
            if (header != null) {
                copied.add(copyHeader(header));
            }
        }
        return copied;
    }

    private static HttpHeader copyHeader(HttpHeader header) {
        return new HttpHeader(header.isEnabled(), header.getKey(), header.getValue());
    }
}

package com.laker.postman.request.model;

import java.util.Locale;

public enum HttpRequestProxyPolicy {
    DEFAULT,
    USE_PROXY,
    NO_PROXY;

    public static HttpRequestProxyPolicy normalize(String policy) {
        if (policy == null || policy.trim().isEmpty()) {
            return DEFAULT;
        }
        String normalized = policy.trim().toUpperCase(Locale.ROOT);
        if (USE_PROXY.name().equals(normalized)) {
            return USE_PROXY;
        }
        if (NO_PROXY.name().equals(normalized)) {
            return NO_PROXY;
        }
        return DEFAULT;
    }

    public static HttpRequestProxyPolicy normalize(HttpRequestProxyPolicy policy) {
        return policy == null ? DEFAULT : policy;
    }
}

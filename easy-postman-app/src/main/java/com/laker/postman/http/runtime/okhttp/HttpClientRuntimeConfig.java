package com.laker.postman.http.runtime.okhttp;

public record HttpClientRuntimeConfig(
        int maxIdleConnections,
        long keepAliveDurationSeconds,
        int maxRequests,
        int maxRequestsPerHost
) {
    public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 6;
    public static final long DEFAULT_KEEP_ALIVE_DURATION_SECONDS = 90L;
    public static final int DEFAULT_MAX_REQUESTS = 64;
    public static final int DEFAULT_MAX_REQUESTS_PER_HOST = 5;

    public HttpClientRuntimeConfig {
        maxIdleConnections = maxIdleConnections > 0 ? maxIdleConnections : DEFAULT_MAX_IDLE_CONNECTIONS;
        keepAliveDurationSeconds = keepAliveDurationSeconds > 0
                ? keepAliveDurationSeconds
                : DEFAULT_KEEP_ALIVE_DURATION_SECONDS;
        maxRequests = maxRequests > 0 ? maxRequests : DEFAULT_MAX_REQUESTS;
        maxRequestsPerHost = maxRequestsPerHost > 0 ? maxRequestsPerHost : DEFAULT_MAX_REQUESTS_PER_HOST;
    }

    public static HttpClientRuntimeConfig defaults() {
        return new HttpClientRuntimeConfig(
                DEFAULT_MAX_IDLE_CONNECTIONS,
                DEFAULT_KEEP_ALIVE_DURATION_SECONDS,
                DEFAULT_MAX_REQUESTS,
                DEFAULT_MAX_REQUESTS_PER_HOST
        );
    }
}

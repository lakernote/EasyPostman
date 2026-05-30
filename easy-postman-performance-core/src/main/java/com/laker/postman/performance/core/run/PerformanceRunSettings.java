package com.laker.postman.performance.core.run;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceRunSettings {
    public static final int DEFAULT_HTTP_MAX_IDLE_CONNECTIONS = 100;
    public static final long DEFAULT_HTTP_KEEP_ALIVE_SECONDS = 60L;
    public static final int DEFAULT_HTTP_MAX_REQUESTS = 1000;
    public static final int DEFAULT_HTTP_MAX_REQUESTS_PER_HOST = 1000;

    boolean efficientMode;
    int httpMaxIdleConnections;
    long httpKeepAliveSeconds;
    int httpMaxRequests;
    int httpMaxRequestsPerHost;

    @Builder
    public PerformanceRunSettings(Boolean efficientMode,
                                  Integer httpMaxIdleConnections,
                                  Long httpKeepAliveSeconds,
                                  Integer httpMaxRequests,
                                  Integer httpMaxRequestsPerHost) {
        this.efficientMode = efficientMode == null || efficientMode;
        this.httpMaxIdleConnections = positive(httpMaxIdleConnections, DEFAULT_HTTP_MAX_IDLE_CONNECTIONS);
        this.httpKeepAliveSeconds = positive(httpKeepAliveSeconds, DEFAULT_HTTP_KEEP_ALIVE_SECONDS);
        this.httpMaxRequests = positive(httpMaxRequests, DEFAULT_HTTP_MAX_REQUESTS);
        this.httpMaxRequestsPerHost = positive(httpMaxRequestsPerHost, DEFAULT_HTTP_MAX_REQUESTS_PER_HOST);
    }

    public static PerformanceRunSettings defaults() {
        return PerformanceRunSettings.builder().build();
    }

    private static int positive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private static long positive(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}

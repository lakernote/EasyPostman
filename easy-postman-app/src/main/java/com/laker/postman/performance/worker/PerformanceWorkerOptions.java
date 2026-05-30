package com.laker.postman.performance.worker;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerOptions {
    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = 19090;
    public static final long DEFAULT_COMPLETED_RUN_RETENTION_MS = 30 * 60 * 1000L;

    boolean help;
    String host;
    int port;
    long completedRunRetentionMs;

    @Builder
    public PerformanceWorkerOptions(Boolean help,
                                    String host,
                                    Integer port,
                                    Long completedRunRetentionMs) {
        this.help = help != null && help;
        this.host = host == null || host.isBlank() ? DEFAULT_HOST : host;
        this.port = port == null ? DEFAULT_PORT : port;
        this.completedRunRetentionMs = Math.max(1L, completedRunRetentionMs == null
                ? DEFAULT_COMPLETED_RUN_RETENTION_MS
                : completedRunRetentionMs);
    }
}

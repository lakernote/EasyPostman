package com.laker.postman.performance.core.worker;

import lombok.Value;

@Value
public class PerformanceWorkerEndpoint {
    String host;
    int port;

    public PerformanceWorkerEndpoint(String host, int port) {
        this.host = host == null || host.isBlank() ? "127.0.0.1" : host;
        this.port = Math.max(0, port);
    }
}

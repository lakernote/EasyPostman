package com.laker.postman.performance.core.worker;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerHealthResponse {
    String status;
    String workerId;
    String host;
    int port;
    String workerProtocolVersion;

    @Builder
    public PerformanceWorkerHealthResponse(String status,
                                           String workerId,
                                           String host,
                                           Integer port,
                                           String workerProtocolVersion) {
        this.status = status == null ? "" : status;
        this.workerId = workerId == null ? "" : workerId;
        this.host = host == null ? "" : host;
        this.port = Math.max(0, port == null ? 0 : port);
        this.workerProtocolVersion = workerProtocolVersion == null ? "" : workerProtocolVersion;
    }

    public boolean usesCurrentProtocol() {
        return PerformanceWorkerProtocol.CURRENT_VERSION.equals(workerProtocolVersion);
    }
}

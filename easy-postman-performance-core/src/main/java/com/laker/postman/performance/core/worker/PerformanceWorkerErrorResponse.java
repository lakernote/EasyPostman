package com.laker.postman.performance.core.worker;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerErrorResponse {
    String error;

    @Builder
    public PerformanceWorkerErrorResponse(String error) {
        this.error = error == null ? "" : error;
    }
}

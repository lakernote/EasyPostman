package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerRunStatusResponse {
    String runId;
    String workerId;
    String status;
    long totalRequests;
    long successRequests;
    long failedRequests;
    String error;

    @Builder
    public PerformanceWorkerRunStatusResponse(String runId,
                                              String workerId,
                                              String status,
                                              Long totalRequests,
                                              Long successRequests,
                                              Long failedRequests,
                                              String error) {
        this.runId = runId == null ? "" : runId;
        this.workerId = workerId == null ? "" : workerId;
        this.status = status == null || status.isBlank() ? PerformanceRunStatus.UNKNOWN : status;
        this.totalRequests = Math.max(0L, totalRequests == null ? 0L : totalRequests);
        this.successRequests = Math.max(0L, successRequests == null ? 0L : successRequests);
        this.failedRequests = Math.max(0L, failedRequests == null ? this.totalRequests - this.successRequests : failedRequests);
        this.error = error == null ? "" : error;
    }
}

package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerRunAcceptedResponse {
    String runId;
    String workerId;
    String status;
    String message;

    @Builder
    public PerformanceWorkerRunAcceptedResponse(String runId, String workerId, String status, String message) {
        this.runId = runId == null ? "" : runId;
        this.workerId = workerId == null ? "" : workerId;
        this.status = status == null || status.isBlank() ? PerformanceRunStatus.ACCEPTED : status;
        this.message = message == null ? "" : message;
    }
}

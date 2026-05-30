package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
public class PerformanceWorkerRunDetailsResponse {
    String runId;
    String workerId;
    String status;
    List<PerformanceWorkerResultDetail> details;
    String error;

    @Builder
    public PerformanceWorkerRunDetailsResponse(String runId,
                                               String workerId,
                                               String status,
                                               List<PerformanceWorkerResultDetail> details,
                                               String error) {
        this.runId = runId == null ? "" : runId;
        this.workerId = workerId == null ? "" : workerId;
        this.status = status == null || status.isBlank() ? PerformanceRunStatus.UNKNOWN : status;
        this.details = details == null ? List.of() : List.copyOf(details);
        this.error = error == null ? "" : error;
    }
}

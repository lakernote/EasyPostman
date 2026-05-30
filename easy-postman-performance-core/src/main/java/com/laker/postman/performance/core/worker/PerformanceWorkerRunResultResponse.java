package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerRunResultResponse {
    String runId;
    String workerId;
    String status;
    PerformanceJsonReport report;
    String error;

    @Builder
    public PerformanceWorkerRunResultResponse(String runId,
                                              String workerId,
                                              String status,
                                              PerformanceJsonReport report,
                                              String error) {
        this.runId = runId == null ? "" : runId;
        this.workerId = workerId == null ? "" : workerId;
        this.status = status == null || status.isBlank() ? PerformanceRunStatus.UNKNOWN : status;
        this.report = report;
        this.error = error == null ? "" : error;
    }
}

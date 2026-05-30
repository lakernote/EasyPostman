package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerRunStatusResponse {
    String runId;
    String workerId;
    String status;
    int activeUsers;
    int totalUsers;
    long totalRequests;
    long successRequests;
    long failedRequests;
    double qps;
    PerformanceJsonReport report;
    String error;

    @Builder
    public PerformanceWorkerRunStatusResponse(String runId,
                                              String workerId,
                                              String status,
                                              Integer activeUsers,
                                              Integer totalUsers,
                                              Long totalRequests,
                                              Long successRequests,
                                              Long failedRequests,
                                              Double qps,
                                              PerformanceJsonReport report,
                                              String error) {
        this.runId = runId == null ? "" : runId;
        this.workerId = workerId == null ? "" : workerId;
        this.status = status == null || status.isBlank() ? PerformanceRunStatus.UNKNOWN : status;
        this.activeUsers = Math.max(0, activeUsers == null ? 0 : activeUsers);
        this.totalUsers = Math.max(0, totalUsers == null ? 0 : totalUsers);
        this.totalRequests = Math.max(0L, totalRequests == null ? 0L : totalRequests);
        this.successRequests = Math.max(0L, successRequests == null ? 0L : successRequests);
        this.failedRequests = Math.max(0L, failedRequests == null ? this.totalRequests - this.successRequests : failedRequests);
        this.qps = Math.max(0.0, qps == null ? 0.0 : qps);
        this.report = report;
        this.error = error == null ? "" : error;
    }
}

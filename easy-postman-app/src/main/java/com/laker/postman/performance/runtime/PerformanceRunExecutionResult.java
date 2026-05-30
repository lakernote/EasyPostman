package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceRunExecutionResult {
    public static final String STATUS_SUCCESS = PerformanceRunStatus.SUCCESS;
    public static final String STATUS_FAILED = PerformanceRunStatus.FAILED;
    public static final String STATUS_STOPPED = PerformanceRunStatus.STOPPED;

    String status;
    String planPath;
    long startTimeMs;
    long endTimeMs;
    long elapsedTimeMs;
    boolean stopped;
    long totalRequests;
    long successRequests;
    long failedRequests;
    String error;
    PerformanceJsonReport report;

    @Builder
    public PerformanceRunExecutionResult(String status,
                                         String planPath,
                                         Long startTimeMs,
                                         Long endTimeMs,
                                         Long elapsedTimeMs,
                                         Boolean stopped,
                                         Long totalRequests,
                                         Long successRequests,
                                         Long failedRequests,
                                         String error,
                                         PerformanceJsonReport report) {
        this.status = status == null || status.isBlank() ? STATUS_SUCCESS : status;
        this.planPath = planPath == null ? "" : planPath;
        this.startTimeMs = startTimeMs == null ? 0L : startTimeMs;
        this.endTimeMs = endTimeMs == null ? 0L : endTimeMs;
        this.elapsedTimeMs = elapsedTimeMs == null ? Math.max(0L, this.endTimeMs - this.startTimeMs) : elapsedTimeMs;
        this.stopped = stopped != null && stopped;
        this.totalRequests = Math.max(0L, totalRequests == null ? 0L : totalRequests);
        this.successRequests = Math.max(0L, successRequests == null ? 0L : successRequests);
        this.failedRequests = Math.max(0L, failedRequests == null ? this.totalRequests - this.successRequests : failedRequests);
        this.error = error == null ? "" : error;
        this.report = report;
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(status);
    }
}

package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceJsonReportMetadata {
    String runId;
    String source;
    String status;
    String planPath;
    long startTimeMs;
    long endTimeMs;
    long elapsedTimeMs;
    boolean stopped;
    String error;

    @Builder
    public PerformanceJsonReportMetadata(String runId,
                                         String source,
                                         String status,
                                         String planPath,
                                         Long startTimeMs,
                                         Long endTimeMs,
                                         Long elapsedTimeMs,
                                         Boolean stopped,
                                         String error) {
        this.runId = runId == null ? "" : runId;
        this.source = source == null || source.isBlank() ? "local" : source;
        this.status = status == null || status.isBlank() ? PerformanceRunStatus.SUCCESS : status;
        this.planPath = planPath == null ? "" : planPath;
        this.startTimeMs = Math.max(0L, startTimeMs == null ? 0L : startTimeMs);
        this.endTimeMs = Math.max(0L, endTimeMs == null ? 0L : endTimeMs);
        this.elapsedTimeMs = elapsedTimeMs == null
                ? Math.max(0L, this.endTimeMs - this.startTimeMs)
                : Math.max(0L, elapsedTimeMs);
        this.stopped = stopped != null && stopped;
        this.error = error == null ? "" : error;
    }
}

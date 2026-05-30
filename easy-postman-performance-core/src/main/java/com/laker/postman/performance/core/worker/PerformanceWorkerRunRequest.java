package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.run.PerformanceRunPlan;
import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceWorkerRunRequest {
    String runId;
    PerformanceRunPlan plan;
    PerformanceWorkerAssignment assignment;

    @Builder
    public PerformanceWorkerRunRequest(String runId,
                                       PerformanceRunPlan plan,
                                       PerformanceWorkerAssignment assignment) {
        this.runId = runId == null ? "" : runId;
        this.plan = plan;
        this.assignment = assignment;
    }
}

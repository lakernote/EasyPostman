package com.laker.postman.performance.core.worker;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class PerformanceWorkerAssignment {
    String schemaVersion;
    String runId;
    String workerId;
    String assignmentId;
    PerformanceWorkerEndpoint endpoint;
    List<PerformanceWorkerThreadGroupAssignment> threadGroups;

    @Builder
    public PerformanceWorkerAssignment(String schemaVersion,
                                       String runId,
                                       String workerId,
                                       String assignmentId,
                                       PerformanceWorkerEndpoint endpoint,
                                       List<PerformanceWorkerThreadGroupAssignment> threadGroups) {
        this.schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? PerformanceWorkerAssignmentJsonStorage.FORMAT_VERSION
                : schemaVersion;
        this.runId = runId == null ? "" : runId;
        this.workerId = workerId == null ? "" : workerId;
        this.assignmentId = assignmentId == null ? "" : assignmentId;
        this.endpoint = endpoint;
        this.threadGroups = copyThreadGroups(threadGroups);
    }

    private static List<PerformanceWorkerThreadGroupAssignment> copyThreadGroups(
            List<PerformanceWorkerThreadGroupAssignment> threadGroups) {
        List<PerformanceWorkerThreadGroupAssignment> copy = new ArrayList<>();
        if (threadGroups == null) {
            return List.of();
        }
        for (PerformanceWorkerThreadGroupAssignment threadGroup : threadGroups) {
            if (threadGroup != null) {
                copy.add(threadGroup);
            }
        }
        return List.copyOf(copy);
    }
}

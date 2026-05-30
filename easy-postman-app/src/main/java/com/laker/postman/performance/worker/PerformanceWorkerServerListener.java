package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;

public interface PerformanceWorkerServerListener {
    PerformanceWorkerServerListener NOOP = new PerformanceWorkerServerListener() {
    };

    default void onRunAccepted(String runId, String workerId) {
    }

    default void onRunAccepted(String runId, String workerId, PerformanceWorkerAssignment assignment) {
        onRunAccepted(runId, workerId);
    }

    default void onRunStarted(String runId, String workerId) {
    }

    default void onRunProgress(String runId,
                               String workerId,
                               String status,
                               int activeUsers,
                               int totalUsers,
                               long totalRequests,
                               long successRequests,
                               long failedRequests,
                               double qps) {
    }

    default void onRunCompleted(String runId,
                                String workerId,
                                String status,
                                PerformanceJsonReportSummary summary,
                                long elapsedMs,
                                String error) {
    }
}

package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceJsonReportStatusResolver {
    private static final String REQUEST_FAILURES_PREFIX = "Request failures: ";

    public String resolve(String requestedStatus,
                          boolean stopped,
                          String error,
                          PerformanceJsonReportSummary summary) {
        if (hasText(error)
                || hasRequestFailures(summary)
                || PerformanceRunStatus.FAILED.equals(requestedStatus)) {
            return PerformanceRunStatus.FAILED;
        }
        if (stopped || PerformanceRunStatus.STOPPED.equals(requestedStatus)) {
            return PerformanceRunStatus.STOPPED;
        }
        return hasText(requestedStatus) ? requestedStatus : PerformanceRunStatus.SUCCESS;
    }

    public String withFailureSummary(String error, PerformanceJsonReportSummary summary) {
        if (hasText(error) || !hasRequestFailures(summary)) {
            return error;
        }
        return REQUEST_FAILURES_PREFIX + summary.getFailedRequests();
    }

    public boolean hasRequestFailures(PerformanceJsonReportSummary summary) {
        return summary != null && summary.getFailedRequests() > 0;
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}

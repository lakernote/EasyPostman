package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.report.PerformanceJsonReport;

@FunctionalInterface
public interface PerformanceRunReportListener {
    PerformanceRunReportListener NOOP = report -> {
    };

    void onReport(PerformanceJsonReport report);
}

package com.laker.postman.performance.master;

import com.laker.postman.performance.core.report.PerformanceJsonReport;

public record PerformanceMasterRunProgress(PerformanceJsonReport report,
                                           int activeUsers,
                                           int totalUsers,
                                           int completedWorkers,
                                           int totalWorkers,
                                           double qps) {
}

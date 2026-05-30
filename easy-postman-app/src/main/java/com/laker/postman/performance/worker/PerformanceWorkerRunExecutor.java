package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.runtime.PerformanceRunExecutionControl;

@FunctionalInterface
public interface PerformanceWorkerRunExecutor {
    PerformanceJsonReport execute(PerformanceWorkerRunRequest request,
                                  PerformanceRunExecutionControl control) throws Exception;
}

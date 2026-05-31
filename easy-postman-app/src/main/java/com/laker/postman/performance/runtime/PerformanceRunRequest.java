package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerformanceRunRequest {
    PerformanceTestPlan plan;
    PerformanceResultSink resultSink;
}

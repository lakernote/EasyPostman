package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;


import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.variable.ExecutionVariableContext;

interface PerformanceRequestRuntime {
    PerformancePreparedRequest prepare(PerformanceRequestSnapshot requestSnapshot,
                                       PerformanceRequestSampler requestSampler,
                                       ExecutionVariableContext iterationContext,
                                       PerformanceExecutionConfig executionConfig);
}

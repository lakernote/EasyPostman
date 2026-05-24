package com.laker.postman.panel.performance.runtime;

import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.execution.PerformanceResultRecorder;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.variable.ExecutionVariableContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;

@Slf4j
@RequiredArgsConstructor
public final class PerformanceSamplerExecutor {

    private final BooleanSupplier runningSupplier;
    private final BooleanSupplier efficientModeSupplier;
    private final PerformanceRequestExecutor requestExecutor;
    private final PerformanceResultRecorder resultRecorder;

    PerformanceRequestExecutionResult execute(PerformanceRequestSampler sampler,
                                              ExecutionVariableContext iterationContext) {
        if (!runningSupplier.getAsBoolean() || sampler == null) {
            return null;
        }

        PerformanceRequestExecutionResult executionResult = requestExecutor.execute(
                sampler,
                iterationContext
        );
        if (executionResult == null) {
            return null;
        }
        resultRecorder.record(executionResult, efficientModeSupplier.getAsBoolean());
        if (executionResult.interrupted) {
            log.debug("请求在停止时被中断: {}", sampler.getName());
        }
        return executionResult;
    }
}

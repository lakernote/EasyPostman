package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.runtime.PerformanceCoreResultSink;


import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import com.laker.postman.service.variable.ExecutionVariableContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
public final class PerformanceSamplerExecutor {

    private final BooleanSupplier runningSupplier;
    private final BooleanSupplier efficientModeSupplier;
    private final PerformanceRequestExecutor requestExecutor;
    private final PerformanceResultCollector resultCollector;
    private final Supplier<PerformanceCoreResultSink> resultSinkSupplier;

    public PerformanceSamplerExecutor(BooleanSupplier runningSupplier,
                                      BooleanSupplier efficientModeSupplier,
                                      PerformanceRequestExecutor requestExecutor,
                                      PerformanceResultCollector resultCollector) {
        this(
                runningSupplier,
                efficientModeSupplier,
                requestExecutor,
                resultCollector,
                () -> PerformanceCoreResultSink.NOOP
        );
    }

    public PerformanceSamplerExecutor(BooleanSupplier runningSupplier,
                                      BooleanSupplier efficientModeSupplier,
                                      PerformanceRequestExecutor requestExecutor,
                                      PerformanceResultCollector resultCollector,
                                      Supplier<PerformanceCoreResultSink> resultSinkSupplier) {
        this.runningSupplier = runningSupplier == null ? () -> false : runningSupplier;
        this.efficientModeSupplier = efficientModeSupplier == null ? () -> false : efficientModeSupplier;
        this.requestExecutor = requestExecutor;
        this.resultCollector = resultCollector == null
                ? new PerformanceResultCollector(PerformanceResultSink.NOOP)
                : resultCollector;
        this.resultSinkSupplier = resultSinkSupplier == null ? () -> PerformanceCoreResultSink.NOOP : resultSinkSupplier;
    }

    PerformanceRequestExecutionResult execute(PerformanceSampler sampler,
                                              ExecutionVariableContext iterationContext) {
        if (!runningSupplier.getAsBoolean() || sampler == null) {
            return null;
        }
        if (!(sampler instanceof PerformanceRequestSampler requestSampler)) {
            log.debug("Unsupported performance sampler type: {}", sampler.getClass().getName());
            return null;
        }

        PerformanceRequestExecutionResult executionResult = requestExecutor.execute(
                requestSampler,
                iterationContext
        );
        if (executionResult == null) {
            return null;
        }
        resultCollector.collect(executionResult, efficientModeSupplier.getAsBoolean(), currentResultSink());
        if (executionResult.interrupted) {
            log.debug("请求在停止时被中断: {}", requestSampler.getName());
        }
        return executionResult;
    }

    private PerformanceCoreResultSink currentResultSink() {
        PerformanceCoreResultSink sink = resultSinkSupplier.get();
        return sink == null ? PerformanceCoreResultSink.NOOP : sink;
    }
}

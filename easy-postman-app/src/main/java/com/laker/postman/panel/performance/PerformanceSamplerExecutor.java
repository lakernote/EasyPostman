package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.execution.PerformanceResultRecorder;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.variable.ExecutionVariableContext;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.function.BooleanSupplier;

@Slf4j
final class PerformanceSamplerExecutor {

    private final BooleanSupplier runningSupplier;
    private final BooleanSupplier efficientModeSupplier;
    private final PerformanceRequestExecutor requestExecutor;
    private final PerformanceResultRecorder resultRecorder;

    PerformanceSamplerExecutor(BooleanSupplier runningSupplier,
                               BooleanSupplier efficientModeSupplier,
                               PerformanceRequestExecutor requestExecutor,
                               PerformanceResultRecorder resultRecorder) {
        this.runningSupplier = runningSupplier;
        this.efficientModeSupplier = efficientModeSupplier;
        this.requestExecutor = requestExecutor;
        this.resultRecorder = resultRecorder;
    }

    PerformanceRequestExecutionResult execute(PerformanceRequestSampler sampler,
                                              ExecutionVariableContext iterationContext) {
        if (!runningSupplier.getAsBoolean() || sampler == null) {
            return null;
        }

        DefaultMutableTreeNode requestNode = sampler.executionTreeNode();
        Object userObj = requestNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode requestData) || requestData.httpRequestItem == null) {
            return null;
        }

        PerformanceRequestExecutionResult executionResult = requestExecutor.execute(
                requestNode,
                requestData,
                iterationContext
        );
        if (executionResult == null) {
            return null;
        }
        resultRecorder.record(executionResult, efficientModeSupplier.getAsBoolean());
        if (executionResult.interrupted) {
            log.debug("请求在停止时被中断: {}", requestData.httpRequestItem.getName());
        }
        return executionResult;
    }
}

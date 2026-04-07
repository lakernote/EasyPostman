package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.ResultNodeInfo;

final class PerformanceResultNodeInfoMapper {

    private PerformanceResultNodeInfoMapper() {
    }

    static ResultNodeInfo toDisplayNodeInfo(PerformanceRequestExecutionResult executionResult) {
        simplifyForDisplay(executionResult);
        return new ResultNodeInfo(
                executionResult.apiName,
                executionResult.errorMsg,
                executionResult.request,
                executionResult.response,
                executionResult.testResults,
                executionResult.executionFailed
        );
    }

    private static void simplifyForDisplay(PerformanceRequestExecutionResult executionResult) {
        executionResult.request.simplify();
        if (executionResult.response != null) {
            executionResult.response.simplify();
            if (!executionResult.request.collectEventInfo) {
                executionResult.response.httpEventInfo = null;
            }
        }
    }
}

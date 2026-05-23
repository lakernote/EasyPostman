package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.panel.performance.model.PerformanceInternalHeaders;
import com.laker.postman.panel.performance.model.ResultNodeInfo;

final class PerformanceResultNodeInfoMapper {

    private PerformanceResultNodeInfoMapper() {
    }

    static ResultNodeInfo toDisplayNodeInfo(PerformanceRequestExecutionResult executionResult) {
        String displayErrorMsg = resolveDisplayErrorMsg(executionResult);
        simplifyForDisplay(executionResult);
        return new ResultNodeInfo(
                executionResult.apiName,
                displayErrorMsg,
                executionResult.request,
                executionResult.response,
                executionResult.testResults,
                executionResult.executionFailed,
                executionResult.protocol
        );
    }

    private static void simplifyForDisplay(PerformanceRequestExecutionResult executionResult) {
        executionResult.request.simplify();
        if (executionResult.response != null) {
            executionResult.response.simplify();
            if (!executionResult.request.collectEventInfo) {
                executionResult.response.httpEventInfo = null;
            }
            PerformanceInternalHeaders.removeInternalHeaders(executionResult.response.headers);
        }
    }

    private static String resolveDisplayErrorMsg(PerformanceRequestExecutionResult executionResult) {
        if (CharSequenceUtil.isNotBlank(executionResult.errorMsg)) {
            return executionResult.errorMsg;
        }
        if (executionResult.response == null) {
            return "";
        }
        return PerformanceInternalHeaders.firstStreamError(executionResult.response.headers);
    }
}

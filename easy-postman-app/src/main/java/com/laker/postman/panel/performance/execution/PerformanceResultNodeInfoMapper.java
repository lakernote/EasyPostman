package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.panel.performance.model.PerformanceInternalHeaders;
import com.laker.postman.panel.performance.model.PerformanceSampleResult;
import com.laker.postman.panel.performance.model.ResultNodeInfo;

final class PerformanceResultNodeInfoMapper {

    private PerformanceResultNodeInfoMapper() {
    }

    static ResultNodeInfo toDisplayNodeInfo(PerformanceRequestExecutionResult executionResult) {
        return toDisplayNodeInfo(PerformanceSampleResult.fromExecutionResult(executionResult));
    }

    static ResultNodeInfo toDisplayNodeInfo(PerformanceSampleResult sampleResult) {
        if (sampleResult == null) {
            return null;
        }
        String displayErrorMsg = resolveDisplayErrorMsg(sampleResult);
        simplifyForDisplay(sampleResult);
        return new ResultNodeInfo(
                sampleResult.getApiName(),
                displayErrorMsg,
                sampleResult.getRequest(),
                sampleResult.getResponse(),
                sampleResult.getTestResults(),
                sampleResult.isExecutionFailed(),
                sampleResult.getProtocol()
        );
    }

    private static void simplifyForDisplay(PerformanceSampleResult sampleResult) {
        if (sampleResult.getRequest() != null) {
            sampleResult.getRequest().simplify();
        }
        if (sampleResult.getResponse() != null) {
            sampleResult.getResponse().simplify();
            if (sampleResult.getRequest() == null || !sampleResult.getRequest().collectEventInfo) {
                sampleResult.getResponse().httpEventInfo = null;
            }
            PerformanceInternalHeaders.removeInternalHeaders(sampleResult.getResponse().headers);
        }
    }

    private static String resolveDisplayErrorMsg(PerformanceSampleResult sampleResult) {
        if (CharSequenceUtil.isNotBlank(sampleResult.getErrorMsg())) {
            return sampleResult.getErrorMsg();
        }
        if (sampleResult.getResponse() == null) {
            return "";
        }
        return PerformanceInternalHeaders.firstStreamError(sampleResult.getResponse().headers);
    }
}

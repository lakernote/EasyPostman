package com.laker.postman.panel.performance.execution;

import com.laker.postman.performance.core.plan.PerformanceAssertionElement;
import com.laker.postman.performance.core.plan.PerformanceExtractorElement;


import com.laker.postman.model.PreparedRequest;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class PerformanceRequestPreparationSupport {

    void configurePreparedRequest(PreparedRequest request, boolean eventLoggingEnabled) {
        request.collectBasicInfo = true;
        request.collectMetricsInfo = true;
        request.collectEventInfo = eventLoggingEnabled;
        request.enableNetworkLog = false;
        request.notifyCookieChanges = false;
    }

    PreparedRequest.ResponseBodyMode resolveHttpResponseBodyModeForAssertionElements(
            boolean efficientMode,
            List<PerformanceAssertionElement> assertionNodes,
            String postscript) {
        return resolveHttpResponseBodyModeForAssertionElements(efficientMode, assertionNodes, List.of(), postscript);
    }

    PreparedRequest.ResponseBodyMode resolveHttpResponseBodyModeForAssertionElements(
            boolean efficientMode,
            List<PerformanceAssertionElement> assertionNodes,
            List<PerformanceExtractorElement> extractorNodes,
            String postscript) {
        return PerformanceResponseCapturePlan.resolveHttpResponseBodyMode(
                efficientMode,
                assertionNodes,
                extractorNodes,
                postscript
        );
    }

    int resolveResponseBodyPreviewLimitBytes(int previewLimitKb) {
        return PerformanceExecutionConfig.responseBodyPreviewLimitBytes(previewLimitKb);
    }
}

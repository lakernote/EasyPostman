package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformanceExtractorElement;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class PerformanceRequestPreparationSupport {

    void configurePreparedRequest(PreparedRequest request) {
        request.collectBasicInfo = true;
        request.collectEventInfo = SettingManager.isPerformanceEventLoggingEnabled();
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
        return SettingManager.performanceResponseBodyPreviewLimitBytes(previewLimitKb);
    }
}

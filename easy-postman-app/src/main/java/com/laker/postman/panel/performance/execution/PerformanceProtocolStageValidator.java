package com.laker.postman.panel.performance.execution;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.plan.PerformancePlanElement;


import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

final class PerformanceProtocolStageValidator {

    private PerformanceProtocolStageValidator() {
    }

    static ValidationResult validate(PerformanceRequestSampler requestSampler, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET && !hasEnabledDirectChild(requestSampler, NodeType.WS_CONNECT)) {
            return ValidationResult.invalid(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_WS_CONNECT_STAGE_REQUIRED));
        }
        if (protocol == PerformanceProtocol.SSE && (!hasEnabledDirectChild(requestSampler, NodeType.SSE_CONNECT)
                || !hasEnabledDirectChild(requestSampler, NodeType.SSE_READ))) {
            return ValidationResult.invalid(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SSE_STAGE_REQUIRED));
        }
        return ValidationResult.ok();
    }

    private static boolean hasEnabledDirectChild(PerformanceRequestSampler requestSampler, NodeType type) {
        if (requestSampler == null || type == null) {
            return false;
        }
        for (PerformancePlanElement element : requestSampler.getChildren()) {
            if (element.getType() == type) {
                return true;
            }
        }
        return false;
    }

    record ValidationResult(boolean valid, String message) {
        private static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        private static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}

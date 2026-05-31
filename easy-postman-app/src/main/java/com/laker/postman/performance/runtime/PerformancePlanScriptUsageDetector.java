package com.laker.postman.performance.runtime;

import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceProtocolStageElement;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import lombok.experimental.UtilityClass;

@UtilityClass
class PerformancePlanScriptUsageDetector {

    boolean usesScripts(PerformanceTestPlan plan) {
        if (plan == null || plan.getThreadGroups().isEmpty()) {
            return false;
        }
        for (PerformanceThreadGroupPlan group : plan.getThreadGroups()) {
            if (group == null) {
                continue;
            }
            for (PerformancePlanElement element : group.getElements()) {
                if (usesScripts(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean usesScripts(PerformancePlanElement element) {
        if (element == null) {
            return false;
        }
        if (element instanceof PerformanceRequestSampler requestSampler) {
            return requestUsesScripts(
                    requestSampler.getRequestSnapshot(),
                    requestSampler.getWebSocketPerformanceData()
            ) || childrenUseScripts(requestSampler);
        }
        if (element instanceof PerformanceCoreRequestSampler requestSampler) {
            return requestUsesScripts(
                    requestSampler.getRequestSnapshot(),
                    requestSampler.getWebSocketPerformanceData()
            ) || childrenUseScripts(requestSampler);
        }
        if (element instanceof PerformanceProtocolStageElement protocolStage) {
            return webSocketUsesScripts(protocolStage.getWebSocketPerformanceData())
                    || elementsUseScripts(protocolStage.getElements());
        }
        if (element instanceof PerformanceController controller) {
            return elementsUseScripts(controller.getElements());
        }
        if (element instanceof PerformanceSampler sampler) {
            return childrenUseScripts(sampler);
        }
        return false;
    }

    private boolean childrenUseScripts(PerformanceSampler sampler) {
        return sampler != null && elementsUseScripts(sampler.getChildren());
    }

    private boolean elementsUseScripts(Iterable<PerformancePlanElement> elements) {
        if (elements == null) {
            return false;
        }
        for (PerformancePlanElement element : elements) {
            if (usesScripts(element)) {
                return true;
            }
        }
        return false;
    }

    private boolean requestUsesScripts(PerformanceRequestSnapshot request,
                                       WebSocketPerformanceData webSocketData) {
        return request != null && (hasText(request.getPrescript()) || hasText(request.getPostscript()))
                || webSocketUsesScripts(webSocketData);
    }

    private boolean webSocketUsesScripts(WebSocketPerformanceData webSocketData) {
        return webSocketData != null && hasText(webSocketData.sendPreScript);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

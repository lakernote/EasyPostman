package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceProtocolStageElement;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.variable.VariableResolver;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class WebSocketScenarioStepSupport {

    boolean hasEnabledReadStep(PerformanceRequestSampler requestSampler) {
        if (requestSampler == null) {
            return false;
        }
        return hasEnabledReadStep(requestSampler.getChildren());
    }

    boolean hasReadStepRequiringPayload(PerformanceRequestSampler requestSampler) {
        if (requestSampler == null) {
            return false;
        }
        return hasReadStepRequiringPayload(requestSampler.getChildren(), requestSampler.getWebSocketPerformanceData());
    }

    boolean hasReadStepWithResponseBodyNode(PerformanceRequestSampler requestSampler) {
        if (requestSampler == null) {
            return false;
        }
        return hasReadStepWithResponseBodyNode(requestSampler.getChildren());
    }

    int maxBufferedMessagesNeededForRead(PerformanceRequestSampler requestSampler) {
        if (requestSampler == null) {
            return 1;
        }
        int required = maxBufferedMessagesNeededForRead(
                requestSampler.getChildren(),
                requestSampler.getWebSocketPerformanceData()
        );
        return required <= 0
                ? WebSocketReceivedMessageBuffer.DEFAULT_MAX_RETAINED_READ_MESSAGES
                : Math.min(required, WebSocketReceivedMessageBuffer.DEFAULT_MAX_RETAINED_READ_MESSAGES);
    }

    WebSocketPerformanceData webSocketData(PerformancePlanElement stepElement,
                                           WebSocketPerformanceData requestConfig) {
        if (stepElement instanceof PerformanceProtocolStageElement stage
                && stage.getWebSocketPerformanceData() != null) {
            return stage.getWebSocketPerformanceData();
        }
        return copyData(requestConfig);
    }

    boolean hasSendPayload(PreparedRequest request,
                           String requestBodyTemplate,
                           WebSocketPerformanceData config) {
        WebSocketPerformanceData.SendContentSource contentSource = contentSource(config);
        return contentSource != WebSocketPerformanceData.SendContentSource.REQUEST_BODY
                || CharSequenceUtil.isNotBlank(resolveSendPayloadTemplate(request, requestBodyTemplate, config));
    }

    ScriptExecutionResult executeSendPreScript(PerformanceScriptRuntime scriptRuntime,
                                               WebSocketPerformanceData config,
                                               int sendIndex,
                                               int sendCount,
                                               String stepName) {
        if (scriptRuntime == null) {
            return ScriptExecutionResult.success();
        }
        String script = config != null ? config.sendPreScript : null;
        if (CharSequenceUtil.isBlank(script)) {
            return ScriptExecutionResult.success();
        }
        return scriptRuntime.executeWebSocketSendScript(script, sendIndex, sendCount, stepName);
    }

    String resolveSendPayload(PreparedRequest request,
                              String requestBodyTemplate,
                              WebSocketPerformanceData config) {
        return VariableResolver.resolve(resolveSendPayloadTemplate(request, requestBodyTemplate, config));
    }

    WebSocketPerformanceData copyData(WebSocketPerformanceData source) {
        WebSocketPerformanceData target = new WebSocketPerformanceData();
        if (source == null) {
            return target;
        }
        target.connectTimeoutMs = source.connectTimeoutMs;
        target.sendMode = source.sendMode;
        target.sendContentSource = contentSource(source);
        target.customSendBody = source.customSendBody;
        target.sendPreScript = source.sendPreScript;
        target.sendCount = source.sendCount;
        target.sendIntervalMs = source.sendIntervalMs;
        target.completionMode = source.completionMode;
        target.firstMessageTimeoutMs = source.firstMessageTimeoutMs;
        target.holdConnectionMs = source.holdConnectionMs;
        target.targetMessageCount = source.targetMessageCount;
        target.messageFilter = source.messageFilter;
        return target;
    }

    private boolean hasEnabledReadStep(List<PerformancePlanElement> elements) {
        for (PerformancePlanElement element : elements) {
            if (element.getType() == NodeType.WS_READ) {
                return true;
            }
            if (element instanceof PerformanceController controller
                    && hasEnabledReadStep(controller.getElements())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReadStepRequiringPayload(List<PerformancePlanElement> elements,
                                                 WebSocketPerformanceData requestConfig) {
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceProtocolStageElement stage && stage.getType() == NodeType.WS_READ) {
                WebSocketPerformanceData cfg = webSocketData(stage, requestConfig);
                if (hasMessageFilter(cfg) || hasResponseBodyNode(stage)) {
                    return true;
                }
            }
            if (element instanceof PerformanceController controller
                    && hasReadStepRequiringPayload(controller.getElements(), requestConfig)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReadStepWithResponseBodyNode(List<PerformancePlanElement> elements) {
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceProtocolStageElement stage
                    && stage.getType() == NodeType.WS_READ
                    && hasResponseBodyNode(stage)) {
                return true;
            }
            if (element instanceof PerformanceController controller
                    && hasReadStepWithResponseBodyNode(controller.getElements())) {
                return true;
            }
        }
        return false;
    }

    private int maxBufferedMessagesNeededForRead(List<PerformancePlanElement> elements,
                                                  WebSocketPerformanceData requestConfig) {
        int max = 0;
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceProtocolStageElement stage && stage.getType() == NodeType.WS_READ) {
                WebSocketPerformanceData cfg = webSocketData(stage, requestConfig);
                max = Math.max(max, bufferedMessagesNeededForRead(cfg));
            }
            if (element instanceof PerformanceController controller) {
                max = Math.max(max, maxBufferedMessagesNeededForRead(controller.getElements(), requestConfig));
            }
        }
        return max;
    }

    private int bufferedMessagesNeededForRead(WebSocketPerformanceData cfg) {
        WebSocketPerformanceData.CompletionMode completionMode = cfg.completionMode == null
                ? WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE
                : cfg.completionMode;
        return switch (completionMode) {
            case SINGLE_MESSAGE, UNTIL_MATCH -> 1;
            case MESSAGE_COUNT -> Math.max(1, cfg.targetMessageCount);
            case FIXED_DURATION -> WebSocketReceivedMessageBuffer.DEFAULT_MAX_RETAINED_READ_MESSAGES;
        };
    }

    private boolean hasMessageFilter(WebSocketPerformanceData cfg) {
        return WebSocketPerformanceData.usesMessageFilter(cfg.completionMode)
                && CharSequenceUtil.isNotBlank(cfg.messageFilter);
    }

    private boolean hasResponseBodyNode(PerformanceProtocolStageElement stage) {
        return PerformanceAssertionRunner.requiresResponseBodyElements(
                PerformanceAssertionRunner.collectDirectAssertionElements(stage.getElements())
        ) || PerformanceExtractorRunner.requiresResponseBodyElements(
                PerformanceExtractorRunner.collectDirectExtractorElements(stage.getElements())
        );
    }

    private String resolveSendPayloadTemplate(PreparedRequest request,
                                              String requestBodyTemplate,
                                              WebSocketPerformanceData config) {
        WebSocketPerformanceData.SendContentSource contentSource = contentSource(config);
        if (contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT) {
            return config == null || config.customSendBody == null ? "" : config.customSendBody;
        }
        if (requestBodyTemplate != null) {
            return requestBodyTemplate;
        }
        return request != null ? request.body : "";
    }

    private WebSocketPerformanceData.SendContentSource contentSource(WebSocketPerformanceData config) {
        return config != null && config.sendContentSource != null
                ? config.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
    }
}

package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceLoopController;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceProtocolStageElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.variable.VariableResolver;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class WebSocketScenarioStepSupport {

    boolean hasEnabledAwaitStep(PerformanceRequestSampler requestSampler) {
        if (requestSampler == null) {
            return false;
        }
        return hasEnabledAwaitStep(requestSampler.getChildren());
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

    ScriptExecutionResult executeSendPreScript(ScriptExecutionPipeline pipeline,
                                               WebSocketPerformanceData config,
                                               int sendIndex,
                                               int sendCount,
                                               String stepName) {
        if (pipeline == null) {
            return ScriptExecutionResult.success();
        }
        String script = config != null ? config.sendPreScript : null;
        return pipeline.executeWebSocketSendScript(script, sendIndex, sendCount, stepName);
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

    private boolean hasEnabledAwaitStep(List<PerformancePlanElement> elements) {
        for (PerformancePlanElement element : elements) {
            if (element.getType() == NodeType.WS_AWAIT) {
                return true;
            }
            if (element instanceof PerformanceLoopController loopController
                    && hasEnabledAwaitStep(loopController.getElements())) {
                return true;
            }
        }
        return false;
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

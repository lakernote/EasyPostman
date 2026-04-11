package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class RequestPreparationService {

    RequestPreparationResult prepare(HttpRequestItem item, boolean useCache) {
        try {
            HttpUtil.ValidationResult protocolValidation = validateProtocol(item);
            if (!protocolValidation.isValid()) {
                return RequestPreparationResult.validationFailure(protocolValidation);
            }

            // 这里把“面板态”转换成真正可发送的 PreparedRequest，后续 helper 都只消费这个对象。
            PreparedRequest request = PreparedRequestBuilder.build(item, useCache);
            ScriptExecutionPipeline pipeline = createScriptPipeline(item, request, useCache);

            String preScriptError = executePreScript(pipeline);
            if (preScriptError != null) {
                return RequestPreparationResult.error(preScriptError);
            }

            // 前置脚本可能会写入变量，所以变量替换必须放到 pre-script 之后再做一次。
            pipeline.finalizeRequest();

            HttpUtil.ValidationResult validationResult = HttpUtil.validateRequest(request, item);
            if (!validationResult.isValid()) {
                return RequestPreparationResult.validationFailure(validationResult);
            }

            return RequestPreparationResult.success(item, request, pipeline, validationResult);
        } catch (Exception ex) {
            log.error("Error preparing request: {}", ex.getMessage(), ex);
            return RequestPreparationResult.error(ex.getMessage());
        }
    }

    private HttpUtil.ValidationResult validateProtocol(HttpRequestItem item) {
        String url = item.getUrl();
        if (item.getProtocol().isWebSocketProtocol()
                && !url.toLowerCase().startsWith("ws://")
                && !url.toLowerCase().startsWith("wss://")) {
            return HttpUtil.ValidationResult.error(
                    I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_WEBSOCKET_PROTOCOL),
                    true
            );
        }
        return HttpUtil.ValidationResult.ok();
    }

    private ScriptExecutionPipeline createScriptPipeline(HttpRequestItem item, PreparedRequest request, boolean useCache) {
        return ScriptExecutionPipeline.forRequestExecution(
                item,
                request,
                new ExecutionVariableContext(),
                useCache
        );
    }

    private String executePreScript(ScriptExecutionPipeline pipeline) {
        ScriptExecutionResult preResult = pipeline.executePreScript();
        if (preResult.isSuccess()) {
            return null;
        }
        return I18nUtil.getMessage(MessageKeys.SCRIPT_PRESCRIPT_EXECUTION_FAILED,
                preResult.getErrorMessage());
    }
}

package com.laker.postman.http.execution;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.http.request.HttpRequestValidationResult;
import com.laker.postman.http.request.HttpRequestValidator;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RequestPreparationService {

    private final JsScriptExecutor.OutputCallback scriptOutputCallback;

    public RequestPreparationService(JsScriptExecutor.OutputCallback scriptOutputCallback) {
        this.scriptOutputCallback = scriptOutputCallback;
    }

    public RequestPreparationResult prepare(HttpRequestItem item, boolean useCache) {
        try {
            HttpRequestValidationResult protocolValidation = validateProtocol(item);
            if (!protocolValidation.isValid()) {
                return RequestPreparationResult.validationFailure(protocolValidation);
            }

            // 这里把“面板态”转换成真正可发送的 PreparedRequest，后续 helper 都只消费这个对象。
            PreparedRequest request = PreparedRequestFactory.build(item, useCache);
            ScriptExecutionPipeline pipeline = createScriptPipeline(item, request, useCache);

            String preScriptError = executePreScript(pipeline);
            if (preScriptError != null) {
                return RequestPreparationResult.error(preScriptError);
            }

            // 前置脚本可能会写入变量，所以变量替换必须放到 pre-script 之后再做一次。
            pipeline.finalizeRequest();

            HttpRequestValidationResult validationResult = HttpRequestValidator.validate(request, item);
            if (!validationResult.isValid()) {
                return RequestPreparationResult.validationFailure(validationResult);
            }

            return RequestPreparationResult.success(item, request, pipeline, validationResult);
        } catch (Exception ex) {
            log.error("Error preparing request: {}", ex.getMessage(), ex);
            return RequestPreparationResult.error(ex.getMessage());
        }
    }

    private HttpRequestValidationResult validateProtocol(HttpRequestItem item) {
        String url = item.getUrl();
        if (item.getProtocol().isWebSocketProtocol()
                && !url.toLowerCase().startsWith("ws://")
                && !url.toLowerCase().startsWith("wss://")) {
            return HttpRequestValidationResult.error(
                    I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_WEBSOCKET_PROTOCOL),
                    true
            );
        }
        return HttpRequestValidationResult.ok();
    }

    private ScriptExecutionPipeline createScriptPipeline(HttpRequestItem item, PreparedRequest request, boolean useCache) {
        return ScriptExecutionPipeline.forRequestExecution(
                item,
                request,
                new ExecutionVariableContext(),
                useCache,
                scriptOutputCallback
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

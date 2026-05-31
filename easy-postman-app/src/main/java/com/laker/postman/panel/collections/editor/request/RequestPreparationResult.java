package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.request.model.HttpRequestItem;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.http.request.HttpRequestValidationResult;
import com.laker.postman.service.js.ScriptExecutionPipeline;

final class RequestPreparationResult {
    private final HttpRequestItem item;
    private final PreparedRequest request;
    private final ScriptExecutionPipeline pipeline;
    private final HttpRequestValidationResult validationResult;
    private final String errorMessage;

    private RequestPreparationResult(HttpRequestItem item,
                                     PreparedRequest request,
                                     ScriptExecutionPipeline pipeline,
                                     HttpRequestValidationResult validationResult,
                                     String errorMessage) {
        this.item = item;
        this.request = request;
        this.pipeline = pipeline;
        this.validationResult = validationResult;
        this.errorMessage = errorMessage;
    }

    static RequestPreparationResult success(HttpRequestItem item,
                                            PreparedRequest request,
                                            ScriptExecutionPipeline pipeline,
                                            HttpRequestValidationResult validationResult) {
        return new RequestPreparationResult(item, request, pipeline, validationResult, null);
    }

    static RequestPreparationResult validationFailure(HttpRequestValidationResult validationResult) {
        return new RequestPreparationResult(null, null, null, validationResult, null);
    }

    static RequestPreparationResult error(String errorMessage) {
        return new RequestPreparationResult(null, null, null, HttpRequestValidationResult.ok(), errorMessage);
    }

    HttpRequestItem getItem() {
        return item;
    }

    PreparedRequest getRequest() {
        return request;
    }

    ScriptExecutionPipeline getPipeline() {
        return pipeline;
    }

    HttpRequestValidationResult getValidationResult() {
        return validationResult;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    boolean hasValidationFailure() {
        return !validationResult.isValid();
    }

    boolean hasWarning() {
        return validationResult.isValid()
                && validationResult.isWarning()
                && CharSequenceUtil.isNotBlank(validationResult.getMessage());
    }

    boolean requiresConfirmation() {
        return validationResult.isValid()
                && validationResult.requiresConfirmation()
                && CharSequenceUtil.isNotBlank(validationResult.getMessage());
    }

    boolean hasError() {
        return CharSequenceUtil.isNotBlank(errorMessage);
    }
}

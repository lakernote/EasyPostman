package com.laker.postman.http.execution;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpRequestItem;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.http.request.HttpRequestValidationResult;
import com.laker.postman.service.js.ScriptExecutionPipeline;

public final class RequestPreparationResult {
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

    public static RequestPreparationResult success(HttpRequestItem item,
                                                   PreparedRequest request,
                                                   ScriptExecutionPipeline pipeline,
                                                   HttpRequestValidationResult validationResult) {
        return new RequestPreparationResult(item, request, pipeline, validationResult, null);
    }

    public static RequestPreparationResult validationFailure(HttpRequestValidationResult validationResult) {
        return new RequestPreparationResult(null, null, null, validationResult, null);
    }

    public static RequestPreparationResult error(String errorMessage) {
        return new RequestPreparationResult(null, null, null, HttpRequestValidationResult.ok(), errorMessage);
    }

    public HttpRequestItem getItem() {
        return item;
    }

    public PreparedRequest getRequest() {
        return request;
    }

    public ScriptExecutionPipeline getPipeline() {
        return pipeline;
    }

    public HttpRequestValidationResult getValidationResult() {
        return validationResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasValidationFailure() {
        return !validationResult.isValid();
    }

    public boolean hasWarning() {
        return validationResult.isValid()
                && validationResult.isWarning()
                && CharSequenceUtil.isNotBlank(validationResult.getMessage());
    }

    public boolean requiresConfirmation() {
        return validationResult.isValid()
                && validationResult.requiresConfirmation()
                && CharSequenceUtil.isNotBlank(validationResult.getMessage());
    }

    public boolean hasError() {
        return CharSequenceUtil.isNotBlank(errorMessage);
    }
}

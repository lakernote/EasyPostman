package com.laker.postman.panel.collections.right.request;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.js.ScriptExecutionPipeline;

final class RequestPreparationResult {
    private final HttpRequestItem item;
    private final PreparedRequest request;
    private final ScriptExecutionPipeline pipeline;
    private final HttpUtil.ValidationResult validationResult;
    private final String errorMessage;

    private RequestPreparationResult(HttpRequestItem item,
                                     PreparedRequest request,
                                     ScriptExecutionPipeline pipeline,
                                     HttpUtil.ValidationResult validationResult,
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
                                            HttpUtil.ValidationResult validationResult) {
        return new RequestPreparationResult(item, request, pipeline, validationResult, null);
    }

    static RequestPreparationResult validationFailure(HttpUtil.ValidationResult validationResult) {
        return new RequestPreparationResult(null, null, null, validationResult, null);
    }

    static RequestPreparationResult error(String errorMessage) {
        return new RequestPreparationResult(null, null, null, HttpUtil.ValidationResult.ok(), errorMessage);
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

    HttpUtil.ValidationResult getValidationResult() {
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

    boolean hasError() {
        return CharSequenceUtil.isNotBlank(errorMessage);
    }
}

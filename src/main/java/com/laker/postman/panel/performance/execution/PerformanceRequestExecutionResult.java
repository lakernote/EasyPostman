package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;

import java.util.List;

public final class PerformanceRequestExecutionResult {

    public final String apiId;
    public final String apiName;
    public final PreparedRequest request;
    public final HttpResponse response;
    public final String errorMsg;
    public final List<TestResult> testResults;
    public final boolean executionFailed;
    public final boolean interrupted;
    public final boolean webSocketRequest;
    public final long requestStartTime;
    public final long fallbackCostMs;

    public PerformanceRequestExecutionResult(String apiId,
                                             String apiName,
                                             PreparedRequest request,
                                             HttpResponse response,
                                             String errorMsg,
                                             List<TestResult> testResults,
                                             boolean executionFailed,
                                             boolean interrupted,
                                             boolean webSocketRequest,
                                             long requestStartTime,
                                             long fallbackCostMs) {
        this.apiId = apiId;
        this.apiName = apiName;
        this.request = request;
        this.response = response;
        this.errorMsg = errorMsg;
        this.testResults = testResults;
        this.executionFailed = executionFailed;
        this.interrupted = interrupted;
        this.webSocketRequest = webSocketRequest;
        this.requestStartTime = requestStartTime;
        this.fallbackCostMs = fallbackCostMs;
    }
}

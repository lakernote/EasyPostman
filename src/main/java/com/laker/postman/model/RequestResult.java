package com.laker.postman.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个请求的执行结果
 */
@Data
public class RequestResult {
    private final String requestName;
    private final String method;
    private final String url;
    private final PreparedRequest req;
    private final HttpResponse response;
    private final long cost;
    private final String status;
    private final AssertionResult assertion;
    private final List<TestResult> testResults;
    private final long timestamp;

    public RequestResult(String requestName, String method, String url,
                         PreparedRequest req, HttpResponse response, long cost, String status,
                         AssertionResult assertion, List<TestResult> testResults) {
        this.requestName = requestName;
        this.method = method;
        this.url = url;
        this.req = req;
        this.response = response;
        this.cost = cost;
        this.status = status;
        this.assertion = assertion;
        this.testResults = testResults != null ? new ArrayList<>(testResults) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
}
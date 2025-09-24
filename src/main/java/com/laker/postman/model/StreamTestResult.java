package com.laker.postman.model;

import java.util.List;

public class StreamTestResult {
    private String message; // 当前流消息内容
    private List<TestResult> testResults; // 本条消息的断言结果
    private long timestamp; // 消息到达时间

    public StreamTestResult(String message, List<TestResult> testResults, long timestamp) {
        this.message = message;
        this.testResults = testResults;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TestResult> getTestResults() {
        return testResults;
    }

    public void setTestResults(List<TestResult> testResults) {
        this.testResults = testResults;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}


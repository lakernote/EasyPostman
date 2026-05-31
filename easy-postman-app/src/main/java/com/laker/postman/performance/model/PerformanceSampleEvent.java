package com.laker.postman.performance.model;

import com.laker.postman.performance.core.model.PerformanceSampleRecord;


import com.laker.postman.performance.execution.PerformanceRequestExecutionResult;
import lombok.Value;

@Value
public class PerformanceSampleEvent {
    PerformanceSampleResult sampleResult;
    PerformanceRequestExecutionResult executionResult;
    boolean efficientMode;

    public PerformanceSampleResult sampleResult() {
        return sampleResult;
    }

    public PerformanceRequestExecutionResult executionResult() {
        return executionResult;
    }

    public PerformanceSampleRecord sampleRecord() {
        return sampleResult == null ? null : sampleResult.toSampleRecord();
    }

    public boolean efficientMode() {
        return efficientMode;
    }
}

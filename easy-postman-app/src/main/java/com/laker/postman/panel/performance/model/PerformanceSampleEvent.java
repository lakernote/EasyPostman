package com.laker.postman.panel.performance.model;

import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
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

    public boolean efficientMode() {
        return efficientMode;
    }
}

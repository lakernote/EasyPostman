package com.laker.postman.panel.performance.model;


import com.laker.postman.model.script.TestResult;
import lombok.Value;

@Value
public class PerformanceAssertionResult {
    String name;
    boolean passed;
    String message;

    public static PerformanceAssertionResult fromTestResult(TestResult testResult) {
        if (testResult == null) {
            return new PerformanceAssertionResult("", false, "");
        }
        return new PerformanceAssertionResult(testResult.name, testResult.passed, testResult.message);
    }
}

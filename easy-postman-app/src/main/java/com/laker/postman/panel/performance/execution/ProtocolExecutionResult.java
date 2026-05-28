package com.laker.postman.panel.performance.execution;


import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;

import java.util.List;

record ProtocolExecutionResult(
        HttpResponse response,
        String errorMsg,
        boolean executionFailed,
        boolean interrupted,
        List<TestResult> testResults
) {
}

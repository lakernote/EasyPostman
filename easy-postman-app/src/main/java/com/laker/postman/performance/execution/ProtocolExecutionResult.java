package com.laker.postman.performance.execution;


import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.TestResult;

import java.util.List;

record ProtocolExecutionResult(
        HttpResponse response,
        String errorMsg,
        boolean executionFailed,
        boolean interrupted,
        List<TestResult> testResults
) {
}

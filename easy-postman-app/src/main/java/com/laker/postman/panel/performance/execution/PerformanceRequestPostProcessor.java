package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Slf4j
@RequiredArgsConstructor
final class PerformanceRequestPostProcessor {

    private final BooleanSupplier runningSupplier;

    PerformanceRequestPostProcessResult process(PerformanceRequestSampler requestSampler,
                                                HttpResponse response,
                                                boolean sseRequest,
                                                boolean webSocketRequest,
                                                ScriptExecutionPipeline pipeline,
                                                String errorMsg,
                                                boolean executionFailed,
                                                List<TestResult> testResults,
                                                PerformanceResponseCapturePlan capturePlan) {
        if (response == null || !runningSupplier.getAsBoolean()) {
            return new PerformanceRequestPostProcessResult(errorMsg, executionFailed);
        }

        String currentErrorMsg = runAssertions(
                requestSampler,
                response,
                sseRequest,
                webSocketRequest,
                errorMsg,
                testResults
        );
        if (capturePlan == null || !capturePlan.runPostScript() || pipeline == null) {
            return new PerformanceRequestPostProcessResult(currentErrorMsg, executionFailed);
        }
        return applyPostScriptResult(
                pipeline.executePostScript(response),
                currentErrorMsg,
                executionFailed,
                testResults
        );
    }

    static PerformanceRequestPostProcessResult applyPostScriptResult(ScriptExecutionResult postResult,
                                                                     String errorMsg,
                                                                     boolean executionFailed,
                                                                     List<TestResult> testResults) {
        String currentErrorMsg = errorMsg;
        boolean currentExecutionFailed = executionFailed;
        if (postResult.hasTestResults()) {
            testResults.addAll(postResult.getTestResults());
            if (!postResult.allTestsPassed()) {
                currentErrorMsg = postResult.getTestResults().stream()
                        .filter(test -> !test.passed)
                        .map(test -> test.name)
                        .findFirst()
                        .orElse("pm.test assertion failed");
            }
        }
        if (!postResult.isSuccess()) {
            log.error("后置脚本执行失败: {}", postResult.getErrorMessage());
            currentErrorMsg = postResult.getErrorMessage();
            currentExecutionFailed = true;
        }
        return new PerformanceRequestPostProcessResult(currentErrorMsg, currentExecutionFailed);
    }

    private String runAssertions(PerformanceRequestSampler requestSampler,
                                 HttpResponse response,
                                 boolean sseRequest,
                                 boolean webSocketRequest,
                                 String errorMsg,
                                 List<TestResult> testResults) {
        List<PerformanceAssertionElement> assertionNodes =
                PerformanceAssertionRunner.collectAssertionElements(requestSampler, sseRequest, webSocketRequest);
        if (assertionNodes.isEmpty()) {
            return errorMsg;
        }
        AtomicReference<String> assertionErrorRef = new AtomicReference<>(errorMsg);
        PerformanceAssertionRunner.runAssertionElements(assertionNodes, response, testResults, assertionErrorRef);
        return assertionErrorRef.get();
    }
}

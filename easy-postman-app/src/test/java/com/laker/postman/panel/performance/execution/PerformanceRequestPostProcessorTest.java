package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.service.js.ScriptExecutionResult;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceRequestPostProcessorTest {

    @Test
    public void shouldSkipPostScriptExecutionWhenScriptIsBlank() {
        PerformanceRequestPostProcessResult result = new PerformanceRequestPostProcessor(() -> true).process(
                null,
                new HttpResponse(),
                false,
                false,
                null,
                "",
                false,
                new ArrayList<>(),
                PerformanceResponseCapturePlan.resolve(true, null, false, false, "")
        );

        assertEquals(result.errorMsg(), "");
        assertFalse(result.executionFailed());
    }

    @Test
    public void shouldUseFirstFailedPmTestNameAsErrorMessage() {
        List<TestResult> testResults = new ArrayList<>();
        ScriptExecutionResult postResult = ScriptExecutionResult.success(List.of(
                new TestResult("ok", true, ""),
                new TestResult("failed assertion", false, "boom")
        ));

        PerformanceRequestPostProcessResult result = PerformanceRequestPostProcessor.applyPostScriptResult(
                postResult,
                "",
                false,
                testResults
        );

        assertEquals(result.errorMsg(), "failed assertion");
        assertFalse(result.executionFailed());
        assertEquals(testResults.size(), 2);
    }

    @Test
    public void shouldMarkExecutionFailedWhenPostScriptFails() {
        List<TestResult> testResults = new ArrayList<>();

        PerformanceRequestPostProcessResult result = PerformanceRequestPostProcessor.applyPostScriptResult(
                ScriptExecutionResult.failure("script boom", null),
                "",
                false,
                testResults
        );

        assertEquals(result.errorMsg(), "script boom");
        assertTrue(result.executionFailed());
        assertTrue(testResults.isEmpty());
    }
}

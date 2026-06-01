package com.laker.postman.performance.execution;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.plan.PerformanceAssertionElement;
import com.laker.postman.performance.core.plan.PerformanceExtractorElement;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.variable.ExecutionVariableContext;
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

    @Test
    public void shouldRunExtractorsBeforeNativeAssertionsAndPostScript() {
        ExtractorData extractorData = new ExtractorData();
        extractorData.type = "JSONPath";
        extractorData.expression = "$.token";
        extractorData.variableName = "token";

        AssertionData assertionData = new AssertionData();
        assertionData.type = "JSONPath";
        assertionData.value = "$.token";
        assertionData.content = "{{token}}";

        PerformanceRequestSampler sampler = new PerformanceRequestSampler(
                "request",
                null,
                null,
                List.of(
                        new PerformanceExtractorElement("token", extractorData),
                        new PerformanceAssertionElement("token assertion", assertionData)
                )
        );
        HttpResponse response = new HttpResponse();
        response.body = "{\"token\":\"abc\"}";
        PreparedRequest request = new PreparedRequest();
        request.postscript = "pm.test('postscript token', function () { pm.expect(pm.variables.get('token')).to.equal('abc'); });";
        ExecutionVariableContext context = new ExecutionVariableContext();
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .postScript(request.postscript)
                .sharedExecutionContext(context)
                .build();
        List<TestResult> testResults = new ArrayList<>();

        PerformanceRequestPostProcessResult result = new PerformanceRequestPostProcessor(() -> true).process(
                sampler,
                response,
                false,
                false,
                new DefaultPerformanceScriptRuntime(pipeline),
                "",
                false,
                testResults,
                PerformanceResponseCapturePlan.resolve(true, sampler, false, false, request.postscript)
        );

        assertEquals(context.getVariables().get("token"), "abc");
        assertEquals(result.errorMsg(), "");
        assertFalse(result.executionFailed());
        assertEquals(testResults.size(), 2);
        assertTrue(testResults.stream().allMatch(testResult -> testResult.passed));
    }
}

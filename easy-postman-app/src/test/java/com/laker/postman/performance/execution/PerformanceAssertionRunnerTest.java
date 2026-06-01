package com.laker.postman.performance.execution;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceAssertionElement;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceProtocolStageElement;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.performance.plan.PerformanceTestPlanCompiler;
import com.laker.postman.performance.plan.PerformanceTestPlanNode;
import com.laker.postman.service.variable.ExecutionContextScope;
import com.laker.postman.service.variable.ExecutionVariableContext;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceAssertionRunnerTest {

    @Test
    public void shouldNotRequireResponseBodyForStatusCodeAssertions() {
        List<PerformanceAssertionElement> assertions = List.of(assertionElement("Response Code"));

        assertFalse(PerformanceAssertionRunner.requiresResponseBodyElements(assertions));
    }

    @Test
    public void shouldRequireResponseBodyForBodyAssertions() {
        assertTrue(PerformanceAssertionRunner.requiresResponseBodyElements(List.of(assertionElement("Contains"))));
        assertTrue(PerformanceAssertionRunner.requiresResponseBodyElements(List.of(assertionElement("JSONPath"))));
    }

    @Test
    public void shouldIgnoreDisabledBodyAssertionsWhenCheckingBodyNeed() {
        PerformanceTestPlanNode requestNode = new PerformanceTestPlanNode(new PerformanceTreeNode("Request", NodeType.REQUEST));
        requestNode.add(assertionTreeNode("Contains", false));
        PerformanceRequestSampler requestSampler = PerformanceTestPlanCompiler.compileRequestSampler(requestNode);

        assertFalse(PerformanceAssertionRunner.requiresResponseBodyElements(
                PerformanceAssertionRunner.collectAssertionElements(requestSampler, false, false)
        ));
    }

    @Test
    public void shouldTreatContainsAssertionOnNullBodyAsFailedAssertion() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionElements(
                List.of(assertionElement("Contains", "ok", "")),
                new HttpResponse(),
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertFalse(results.get(0).passed);
        assertFalse(error.get().isBlank());
    }

    @Test
    public void shouldTreatBlankContainsAssertionOnNullBodyAsFailedAssertion() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionElements(
                List.of(assertionElement("Contains", "", "")),
                new HttpResponse(),
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertFalse(results.get(0).passed);
        assertFalse(error.get().isBlank());
    }

    @Test
    public void shouldTreatJsonPathAssertionOnNullBodyAsFailedAssertion() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionElements(
                List.of(assertionElement("JSONPath", "alice", "$.name")),
                new HttpResponse(),
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertFalse(results.get(0).passed);
        assertFalse(error.get().isBlank());
    }

    @Test
    public void shouldRunJsonPathAssertionAgainstSseDataPayload() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");
        HttpResponse response = new HttpResponse();
        response.isSse = true;
        response.body = "event: done\n"
                + "data: {\"name\":\"target\"}\n\n";

        PerformanceAssertionRunner.runAssertionElements(
                List.of(assertionElement("JSONPath", "target", "$.name")),
                response,
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertTrue(results.get(0).passed);
        assertEquals(error.get(), "");
    }

    @Test
    public void shouldCollectAssertionsFromAllSseReadStages() {
        PerformanceRequestSampler sampler = new PerformanceRequestSampler(
                "sse",
                null,
                null,
                List.of(
                        stage(NodeType.SSE_READ, List.of(assertionElement("Contains", "first", ""))),
                        stage(NodeType.SSE_READ, List.of(assertionElement("Contains", "second", "")))
                )
        );

        List<PerformanceAssertionElement> assertions =
                PerformanceAssertionRunner.collectAssertionElements(sampler, true, false);

        assertEquals(assertions.size(), 2);
        assertEquals(assertions.get(0).getAssertionData().content, "first");
        assertEquals(assertions.get(1).getAssertionData().content, "second");
    }

    @Test
    public void shouldTreatStatusAssertionOnNullResponseAsFailedAssertion() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionElements(
                List.of(assertionElement("Response Code", "", "200")),
                null,
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertFalse(results.get(0).passed);
        assertFalse(error.get().isBlank());
    }

    @Test
    public void shouldRunCommonNonBodyAssertions() {
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.costMs = 88;
        response.bodySize = 12;
        response.headers = new LinkedHashMap<>();
        response.addHeader("X-Trace", List.of("trace-001"));
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionElements(
                List.of(
                        assertionElement("Response Time", "", "100", "<"),
                        assertionElement("Header Exists", "X-Trace", ""),
                        assertionElement("Header Equals", "x-trace", "trace-001"),
                        assertionElement("Body Size", "", "12", "=")
                ),
                response,
                results,
                error
        );

        assertEquals(results.size(), 4);
        assertTrue(results.stream().allMatch(result -> result.passed));
        assertEquals(error.get(), "");
    }

    @Test
    public void shouldRunRegexAssertionAgainstResponseBody() {
        HttpResponse response = new HttpResponse();
        response.body = "{\"token\":\"abc\"}";
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionElements(
                List.of(assertionElement("Regex", "\"token\":\"\\w+\"", "")),
                response,
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertTrue(results.get(0).passed);
    }

    @Test
    public void shouldTreatInvalidRegexAsFailedAssertion() {
        HttpResponse response = new HttpResponse();
        response.body = "{\"token\":\"abc\"}";
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionElements(
                List.of(assertionElement("Regex", "[", "")),
                response,
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertFalse(results.get(0).passed);
        assertFalse(error.get().isBlank());
    }

    @Test
    public void shouldResolveAssertionFieldsFromExecutionVariables() {
        HttpResponse response = new HttpResponse();
        response.body = "{\"token\":\"abc\"}";
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");
        ExecutionVariableContext context = new ExecutionVariableContext(Map.of("token", "abc"), Map.of());

        try (ExecutionContextScope ignored = ExecutionContextScope.open(context)) {
            PerformanceAssertionRunner.runAssertionElements(
                    List.of(assertionElement("JSONPath", "{{token}}", "$.token")),
                    response,
                    results,
                    error
            );
        }

        assertEquals(results.size(), 1);
        assertTrue(results.get(0).passed);
        assertEquals(error.get(), "");
    }

    private static PerformanceAssertionElement assertionElement(String type) {
        return assertionElement(type, "", "");
    }

    private static PerformanceProtocolStageElement stage(NodeType type, List<PerformancePlanElement> elements) {
        return new PerformanceProtocolStageElement(type.name(), type, null, null, elements);
    }

    private static PerformanceAssertionElement assertionElement(String type, String content, String value) {
        return new PerformanceAssertionElement(type, assertionData(type, content, value));
    }

    private static PerformanceAssertionElement assertionElement(String type, String content, String value, String operator) {
        AssertionData data = assertionData(type, content, value);
        data.operator = operator;
        return new PerformanceAssertionElement(type, data);
    }

    private static PerformanceTestPlanNode assertionTreeNode(String type, boolean enabled) {
        AssertionData data = new AssertionData();
        data.type = type;
        PerformanceTreeNode node = new PerformanceTreeNode(type, NodeType.ASSERTION, data);
        node.enabled = enabled;
        return new PerformanceTestPlanNode(node);
    }

    private static AssertionData assertionData(String type, String content, String value) {
        AssertionData data = new AssertionData();
        data.type = type;
        data.content = content;
        data.value = value;
        return data;
    }
}

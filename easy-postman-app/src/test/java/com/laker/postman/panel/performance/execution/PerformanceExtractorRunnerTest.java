package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.panel.performance.extractor.ExtractorData;
import com.laker.postman.panel.performance.extractor.ExtractorType;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.plan.PerformanceExtractorElement;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceProtocolStageElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.variable.ExecutionContextScope;
import com.laker.postman.service.variable.ExecutionVariableContext;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PerformanceExtractorRunnerTest {

    @Test
    public void shouldExtractValuesIntoExecutionVariables() {
        HttpResponse response = response();
        response.body = "{\"token\":\"abc\",\"user\":{\"id\":42}}";
        response.headers = new LinkedHashMap<>();
        response.addHeader("X-Trace", List.of("trace-001"));
        response.addHeader("Set-Cookie", List.of("sid=s-123; Path=/; HttpOnly"));

        ExecutionVariableContext context = new ExecutionVariableContext();
        try (ExecutionContextScope ignored = ExecutionContextScope.open(context)) {
            PerformanceExtractorRunner.runExtractorElements(List.of(
                    extractor("JSONPath", "$.token", "token"),
                    extractor("Header", "x-trace", "trace"),
                    extractor("Cookie", "sid", "sid"),
                    regexExtractor("Regex", "\"id\":(\\d+)", "userId", 1)
            ), response);
        }

        assertEquals(context.getVariables().get("token"), "abc");
        assertEquals(context.getVariables().get("trace"), "trace-001");
        assertEquals(context.getVariables().get("sid"), "s-123");
        assertEquals(context.getVariables().get("userId"), "42");
    }

    @Test
    public void shouldUseDefaultValueWhenExtractionMisses() {
        HttpResponse response = response();
        response.body = "{}";

        ExtractorData data = new ExtractorData();
        data.type = ExtractorType.JSON_PATH.getStorageValue();
        data.expression = "$.missing";
        data.variableName = "missing";
        data.defaultValue = "fallback";

        ExecutionVariableContext context = new ExecutionVariableContext();
        try (ExecutionContextScope ignored = ExecutionContextScope.open(context)) {
            PerformanceExtractorRunner.runExtractorElements(List.of(new PerformanceExtractorElement("missing", data)), response);
        }

        assertEquals(context.getVariables().get("missing"), "fallback");
    }

    @Test
    public void shouldRequireResponseBodyOnlyForBodyBasedExtractors() {
        assertFalse(PerformanceExtractorRunner.requiresResponseBodyElements(List.of(
                extractor("Header", "X-Trace", "trace"),
                extractor("Cookie", "sid", "sid")
        )));
        org.testng.Assert.assertTrue(PerformanceExtractorRunner.requiresResponseBodyElements(List.of(
                extractor("JSONPath", "$.token", "token"),
                extractor("Regex", "token=(\\w+)", "token")
        )));
    }

    @Test
    public void shouldCollectExtractorsFromAllSseReadStages() {
        PerformanceRequestSampler sampler = new PerformanceRequestSampler(
                "sse",
                null,
                null,
                null,
                List.of(
                        stage(NodeType.SSE_READ, List.of(extractor("Header", "X-A", "first"))),
                        stage(NodeType.SSE_READ, List.of(extractor("Header", "X-B", "second")))
                )
        );

        List<PerformanceExtractorElement> extractors =
                PerformanceExtractorRunner.collectExtractorElements(sampler, true, false);

        assertEquals(extractors.size(), 2);
        assertEquals(extractors.get(0).getExtractorData().variableName, "first");
        assertEquals(extractors.get(1).getExtractorData().variableName, "second");
    }

    private static HttpResponse response() {
        HttpResponse response = new HttpResponse();
        response.code = 200;
        return response;
    }

    private static PerformanceProtocolStageElement stage(NodeType type, List<PerformancePlanElement> elements) {
        return new PerformanceProtocolStageElement(type.name(), type, null, null, elements);
    }

    private static PerformanceExtractorElement extractor(String type, String expression, String variableName) {
        ExtractorData data = new ExtractorData();
        data.type = type;
        data.expression = expression;
        data.variableName = variableName;
        return new PerformanceExtractorElement(variableName, data);
    }

    private static PerformanceExtractorElement regexExtractor(String type, String expression, String variableName, int groupIndex) {
        ExtractorData data = new ExtractorData();
        data.type = type;
        data.expression = expression;
        data.variableName = variableName;
        data.groupIndex = groupIndex;
        return new PerformanceExtractorElement(variableName, data);
    }
}

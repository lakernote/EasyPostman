package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceTestPlanCompiler;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
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
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new JMeterTreeNode("Request", NodeType.REQUEST));
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

    private static PerformanceAssertionElement assertionElement(String type) {
        return assertionElement(type, "", "");
    }

    private static PerformanceAssertionElement assertionElement(String type, String content, String value) {
        return new PerformanceAssertionElement(type, assertionData(type, content, value));
    }

    private static DefaultMutableTreeNode assertionTreeNode(String type, boolean enabled) {
        AssertionData data = new AssertionData();
        data.type = type;
        JMeterTreeNode node = new JMeterTreeNode(type, NodeType.ASSERTION, data);
        node.enabled = enabled;
        return new DefaultMutableTreeNode(node);
    }

    private static AssertionData assertionData(String type, String content, String value) {
        AssertionData data = new AssertionData();
        data.type = type;
        data.content = content;
        data.value = value;
        return data;
    }
}

package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
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
        List<DefaultMutableTreeNode> assertions = List.of(assertionNode("Response Code", true));

        assertFalse(PerformanceAssertionRunner.requiresResponseBody(assertions));
    }

    @Test
    public void shouldRequireResponseBodyForBodyAssertions() {
        assertTrue(PerformanceAssertionRunner.requiresResponseBody(List.of(assertionNode("Contains", true))));
        assertTrue(PerformanceAssertionRunner.requiresResponseBody(List.of(assertionNode("JSONPath", true))));
    }

    @Test
    public void shouldIgnoreDisabledBodyAssertionsWhenCheckingBodyNeed() {
        List<DefaultMutableTreeNode> assertions = List.of(assertionNode("Contains", false));

        assertFalse(PerformanceAssertionRunner.requiresResponseBody(assertions));
    }

    @Test
    public void shouldTreatContainsAssertionOnNullBodyAsFailedAssertion() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<String> error = new AtomicReference<>("");

        PerformanceAssertionRunner.runAssertionNodes(
                List.of(assertionNode("Contains", true, "ok", "")),
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

        PerformanceAssertionRunner.runAssertionNodes(
                List.of(assertionNode("Contains", true, "", "")),
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

        PerformanceAssertionRunner.runAssertionNodes(
                List.of(assertionNode("JSONPath", true, "alice", "$.name")),
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

        PerformanceAssertionRunner.runAssertionNodes(
                List.of(assertionNode("Response Code", true, "", "200")),
                null,
                results,
                error
        );

        assertEquals(results.size(), 1);
        assertFalse(results.get(0).passed);
        assertFalse(error.get().isBlank());
    }

    private static DefaultMutableTreeNode assertionNode(String type, boolean enabled) {
        return assertionNode(type, enabled, "", "");
    }

    private static DefaultMutableTreeNode assertionNode(String type, boolean enabled, String content, String value) {
        AssertionData data = new AssertionData();
        data.type = type;
        data.content = content;
        data.value = value;
        JMeterTreeNode node = new JMeterTreeNode(type, NodeType.ASSERTION, data);
        node.enabled = enabled;
        return new DefaultMutableTreeNode(node);
    }
}

package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

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

    private static DefaultMutableTreeNode assertionNode(String type, boolean enabled) {
        AssertionData data = new AssertionData();
        data.type = type;
        JMeterTreeNode node = new JMeterTreeNode(type, NodeType.ASSERTION, data);
        node.enabled = enabled;
        return new DefaultMutableTreeNode(node);
    }
}

package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceProtocolStageValidatorTest {

    @Test(description = "WebSocket 请求必须有启用的请求级 WS Connect 阶段")
    public void shouldRequireEnabledDirectWebSocketConnectStage() {
        DefaultMutableTreeNode requestNode = requestNode();
        DefaultMutableTreeNode loopNode = new DefaultMutableTreeNode(new JMeterTreeNode("Loop", NodeType.LOOP));
        loopNode.add(node(NodeType.WS_CONNECT, true));
        requestNode.add(loopNode);

        assertFalse(PerformanceProtocolStageValidator.validate(requestNode, PerformanceProtocol.WEBSOCKET).valid());

        DefaultMutableTreeNode disabledConnect = node(NodeType.WS_CONNECT, false);
        requestNode.add(disabledConnect);

        assertFalse(PerformanceProtocolStageValidator.validate(requestNode, PerformanceProtocol.WEBSOCKET).valid());

        DefaultMutableTreeNode enabledConnect = node(NodeType.WS_CONNECT, true);
        requestNode.add(enabledConnect);

        assertTrue(PerformanceProtocolStageValidator.validate(requestNode, PerformanceProtocol.WEBSOCKET).valid());
    }

    @Test(description = "SSE 请求必须同时有启用的 Connect 和 Receive 阶段")
    public void shouldRequireEnabledDirectSseStages() {
        DefaultMutableTreeNode requestNode = requestNode();

        assertFalse(PerformanceProtocolStageValidator.validate(requestNode, PerformanceProtocol.SSE).valid());

        requestNode.add(node(NodeType.SSE_CONNECT, true));
        requestNode.add(node(NodeType.SSE_AWAIT, false));

        assertFalse(PerformanceProtocolStageValidator.validate(requestNode, PerformanceProtocol.SSE).valid());

        requestNode.add(node(NodeType.SSE_AWAIT, true));

        assertTrue(PerformanceProtocolStageValidator.validate(requestNode, PerformanceProtocol.SSE).valid());
    }

    @Test(description = "HTTP 请求不需要协议阶段节点")
    public void shouldNotRequireProtocolStagesForHttp() {
        assertTrue(PerformanceProtocolStageValidator.validate(requestNode(), PerformanceProtocol.HTTP).valid());
    }

    private static DefaultMutableTreeNode requestNode() {
        return new DefaultMutableTreeNode(new JMeterTreeNode("Request", NodeType.REQUEST));
    }

    private static DefaultMutableTreeNode node(NodeType type, boolean enabled) {
        JMeterTreeNode data = new JMeterTreeNode(type.name(), type);
        data.enabled = enabled;
        return new DefaultMutableTreeNode(data);
    }
}

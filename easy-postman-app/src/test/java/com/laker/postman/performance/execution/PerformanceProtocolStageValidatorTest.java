package com.laker.postman.performance.execution;

import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.plan.PerformanceTestPlanCompiler;
import com.laker.postman.performance.plan.PerformanceTestPlanNode;
import org.testng.annotations.Test;


import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceProtocolStageValidatorTest {

    @Test(description = "WebSocket 请求必须有启用的请求级 WS Connect 阶段")
    public void shouldRequireEnabledDirectWebSocketConnectStage() {
        PerformanceTestPlanNode requestNode = requestNode();
        PerformanceTestPlanNode loopNode = new PerformanceTestPlanNode(new PerformanceTreeNode("Loop", NodeType.LOOP));
        loopNode.add(node(NodeType.WS_CONNECT, true));
        requestNode.add(loopNode);

        assertFalse(validateRequest(requestNode, PerformanceProtocol.WEBSOCKET).valid());

        PerformanceTestPlanNode disabledConnect = node(NodeType.WS_CONNECT, false);
        requestNode.add(disabledConnect);

        assertFalse(validateRequest(requestNode, PerformanceProtocol.WEBSOCKET).valid());

        PerformanceTestPlanNode enabledConnect = node(NodeType.WS_CONNECT, true);
        requestNode.add(enabledConnect);

        assertTrue(validateRequest(requestNode, PerformanceProtocol.WEBSOCKET).valid());
    }

    @Test(description = "SSE 请求必须同时有启用的 Connect 和 Receive 阶段")
    public void shouldRequireEnabledDirectSseStages() {
        PerformanceTestPlanNode requestNode = requestNode();

        assertFalse(validateRequest(requestNode, PerformanceProtocol.SSE).valid());

        requestNode.add(node(NodeType.SSE_CONNECT, true));
        requestNode.add(node(NodeType.SSE_READ, false));

        assertFalse(validateRequest(requestNode, PerformanceProtocol.SSE).valid());

        requestNode.add(node(NodeType.SSE_READ, true));

        assertTrue(validateRequest(requestNode, PerformanceProtocol.SSE).valid());
    }

    @Test(description = "HTTP 请求不需要协议阶段节点")
    public void shouldNotRequireProtocolStagesForHttp() {
        assertTrue(validateRequest(requestNode(), PerformanceProtocol.HTTP).valid());
    }

    private static PerformanceTestPlanNode requestNode() {
        return new PerformanceTestPlanNode(new PerformanceTreeNode("Request", NodeType.REQUEST));
    }

    private static PerformanceTestPlanNode node(NodeType type, boolean enabled) {
        PerformanceTreeNode data = new PerformanceTreeNode(type.name(), type);
        data.enabled = enabled;
        return new PerformanceTestPlanNode(data);
    }

    private static PerformanceProtocolStageValidator.ValidationResult validateRequest(PerformanceTestPlanNode requestNode,
                                                                                     PerformanceProtocol protocol) {
        return PerformanceProtocolStageValidator.validate(
                PerformanceTestPlanCompiler.compileRequestSampler(requestNode),
                protocol
        );
    }
}

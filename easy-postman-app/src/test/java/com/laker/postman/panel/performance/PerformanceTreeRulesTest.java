package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeRulesTest {

    @Test(description = "共享树规则应统一约束协议阶段节点的放置位置")
    public void shouldApplyProtocolStagePlacementRules() {
        DefaultMutableTreeNode wsRequest = newRequestNode(RequestItemProtocolEnum.WEBSOCKET);
        DefaultMutableTreeNode wsLoop = newLoopNode();
        wsRequest.add(wsLoop);

        assertTrue(PerformanceTreeRules.canAcceptChild(wsRequest, newNode(NodeType.WS_CONNECT)));
        assertFalse(PerformanceTreeRules.canAcceptChild(wsLoop, newNode(NodeType.WS_CONNECT)));
        assertTrue(PerformanceTreeRules.canAcceptChild(wsLoop, newNode(NodeType.WS_SEND)));

        DefaultMutableTreeNode sseRequest = newRequestNode(RequestItemProtocolEnum.HTTP);
        ((JMeterTreeNode) sseRequest.getUserObject()).httpRequestItem.setHeadersList(List.of(
                new HttpHeader(true, "Accept", "text/event-stream")
        ));

        assertTrue(PerformanceTreeRules.canAcceptChild(sseRequest, newNode(NodeType.SSE_CONNECT)));
        assertTrue(PerformanceTreeRules.canAcceptChild(sseRequest, newNode(NodeType.SSE_AWAIT)));
        assertTrue(PerformanceTreeRules.canAcceptChild(sseAwaitNode(), newNode(NodeType.EXTRACTOR)));
        assertTrue(PerformanceTreeRules.canAcceptChild(newRequestNode(RequestItemProtocolEnum.HTTP), newNode(NodeType.EXTRACTOR)));
        assertFalse(PerformanceTreeRules.canAcceptChild(wsRequest, newNode(NodeType.EXTRACTOR)));
    }

    @Test(description = "CSV Data Set 只能挂在线程组下，作为线程组作用域配置")
    public void shouldOnlyAcceptCsvDataSetUnderThreadGroup() {
        DefaultMutableTreeNode root = newNode(NodeType.ROOT);
        DefaultMutableTreeNode threadGroup = newNode(NodeType.THREAD_GROUP);
        DefaultMutableTreeNode request = newRequestNode(RequestItemProtocolEnum.HTTP);
        DefaultMutableTreeNode loop = newLoopNode();

        assertTrue(PerformanceTreeRules.canAcceptChild(threadGroup, newNode(NodeType.CSV_DATA_SET)));
        assertFalse(PerformanceTreeRules.canAcceptChild(root, newNode(NodeType.CSV_DATA_SET)));
        assertFalse(PerformanceTreeRules.canAcceptChild(request, newNode(NodeType.CSV_DATA_SET)));
        assertFalse(PerformanceTreeRules.canAcceptChild(loop, newNode(NodeType.CSV_DATA_SET)));
    }

    private static DefaultMutableTreeNode newRequestNode(RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setProtocol(protocol);
        return new DefaultMutableTreeNode(new JMeterTreeNode(protocol.name(), NodeType.REQUEST, item));
    }

    private static DefaultMutableTreeNode newLoopNode() {
        JMeterTreeNode loopData = new JMeterTreeNode("Loop", NodeType.LOOP);
        loopData.loopData = new LoopData();
        return new DefaultMutableTreeNode(loopData);
    }

    private static DefaultMutableTreeNode newNode(NodeType type) {
        return new DefaultMutableTreeNode(new JMeterTreeNode(type.name(), type));
    }

    private static DefaultMutableTreeNode sseAwaitNode() {
        return new DefaultMutableTreeNode(new JMeterTreeNode("SSE Await", NodeType.SSE_AWAIT));
    }
}

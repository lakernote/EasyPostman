package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

public class PerformanceTreeSupportTest {

    @Test(description = "SSE 请求应自动补齐固定节点，并把断言归并到 await 节点下")
    public void shouldCreateSseStructureAndMoveAssertionsUnderAwaitNode() {
        TestContext context = newTestContext(RequestItemProtocolEnum.SSE);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        context.treeModel.insertNodeInto(assertionNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.SSE_CONNECT, NodeType.SSE_AWAIT));
        DefaultMutableTreeNode awaitNode = findChild(context.requestNode, NodeType.SSE_AWAIT);
        assertNotNull(awaitNode);
        assertEquals(childTypesOf(awaitNode), List.of(NodeType.ASSERTION));
        assertSame(awaitNode.getChildAt(0), assertionNode);
        assertNotNull(context.requestData.ssePerformanceData);
    }

    @Test(description = "SSE 请求切回 HTTP 后应移除 SSE 固定节点，并把断言恢复到请求节点下")
    public void shouldRemoveSseNodesAndRestoreAssertionsWhenSwitchingBackToHttp() {
        TestContext context = newTestContext(RequestItemProtocolEnum.SSE);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        context.treeModel.insertNodeInto(assertionNode, context.requestNode, context.requestNode.getChildCount());
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        context.requestData.httpRequestItem.setProtocol(RequestItemProtocolEnum.HTTP);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.ASSERTION));
        assertSame(context.requestNode.getChildAt(0), assertionNode);
        assertNull(findChild(context.requestNode, NodeType.SSE_CONNECT));
        assertNull(findChild(context.requestNode, NodeType.SSE_AWAIT));
    }

    @Test(description = "WebSocket 请求应创建连接节点并初始化默认压测数据")
    public void shouldCreateWebSocketConnectNodeAndDefaults() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT));
        assertNotNull(context.requestData.webSocketPerformanceData);
        assertNotNull(findChild(context.requestNode, NodeType.WS_CONNECT));
    }

    @Test(description = "WebSocket 请求切回 HTTP 后应清理步骤节点，并把 await 下的断言恢复出来")
    public void shouldRemoveWebSocketNodesAndRestoreAssertionsWhenSwitchingBackToHttp() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode awaitNode = newNode("Await", NodeType.WS_AWAIT);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        awaitNode.add(assertionNode);
        context.treeModel.insertNodeInto(newNode("Send", NodeType.WS_SEND), context.requestNode, context.requestNode.getChildCount());
        context.treeModel.insertNodeInto(awaitNode, context.requestNode, context.requestNode.getChildCount());
        context.treeModel.insertNodeInto(newNode("Close", NodeType.WS_CLOSE), context.requestNode, context.requestNode.getChildCount());

        context.requestData.httpRequestItem.setProtocol(RequestItemProtocolEnum.HTTP);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.ASSERTION));
        assertSame(context.requestNode.getChildAt(0), assertionNode);
        assertNull(findChild(context.requestNode, NodeType.WS_CONNECT));
        assertNull(findChild(context.requestNode, NodeType.WS_SEND));
        assertNull(findChild(context.requestNode, NodeType.WS_AWAIT));
        assertNull(findChild(context.requestNode, NodeType.WS_CLOSE));
    }

    private static TestContext newTestContext(RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setName("Request");
        item.setProtocol(protocol);
        JMeterTreeNode requestData = new JMeterTreeNode("Request", NodeType.REQUEST, item);
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);
        root.add(requestNode);
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        return new TestContext(new PerformanceTreeSupport(treeModel), treeModel, requestNode, requestData);
    }

    private static DefaultMutableTreeNode newNode(String name, NodeType type) {
        return new DefaultMutableTreeNode(new JMeterTreeNode(name, type));
    }

    private static DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, NodeType type) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof JMeterTreeNode node && node.type == type) {
                return child;
            }
        }
        return null;
    }

    private static List<NodeType> childTypesOf(DefaultMutableTreeNode parent) {
        List<NodeType> types = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            Object userObject = ((DefaultMutableTreeNode) parent.getChildAt(i)).getUserObject();
            if (userObject instanceof JMeterTreeNode node) {
                types.add(node.type);
            }
        }
        return types;
    }

    private record TestContext(
            PerformanceTreeSupport treeSupport,
            DefaultTreeModel treeModel,
            DefaultMutableTreeNode requestNode,
            JMeterTreeNode requestData
    ) {
    }
}

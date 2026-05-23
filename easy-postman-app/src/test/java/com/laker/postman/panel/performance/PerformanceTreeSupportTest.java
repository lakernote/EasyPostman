package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import org.testng.annotations.Test;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

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

    @Test(description = "SSE 匹配消息模式应在 Receive 节点标题展示消息过滤条件")
    public void shouldShowSseMatchedMessageFilterInAwaitNodeTitle() {
        TestContext context = newTestContext(RequestItemProtocolEnum.SSE);
        context.requestData.ssePerformanceData = new SsePerformanceData();
        context.requestData.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.MATCHED_MESSAGE;
        context.requestData.ssePerformanceData.firstMessageTimeoutMs = 10000;
        context.requestData.ssePerformanceData.messageFilter = "done";

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode awaitNode = findChild(context.requestNode, NodeType.SSE_AWAIT);
        assertNotNull(awaitNode);
        JMeterTreeNode awaitData = (JMeterTreeNode) awaitNode.getUserObject();
        assertTrue(awaitData.name.contains("10s"), awaitData.name);
        assertTrue(awaitData.name.contains("contains=done"), awaitData.name);
    }

    @Test(description = "SSE 固定时长模式不应在 Receive 节点标题展示事件过滤条件")
    public void shouldHideSseEventFilterInFixedDurationAwaitNodeTitle() {
        TestContext context = newTestContext(RequestItemProtocolEnum.SSE);
        context.requestData.ssePerformanceData = new SsePerformanceData();
        context.requestData.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.FIXED_DURATION;
        context.requestData.ssePerformanceData.holdConnectionMs = 30000;
        context.requestData.ssePerformanceData.eventNameFilter = "done";

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode awaitNode = findChild(context.requestNode, NodeType.SSE_AWAIT);
        assertNotNull(awaitNode);
        JMeterTreeNode awaitData = (JMeterTreeNode) awaitNode.getUserObject();
        assertTrue(awaitData.name.contains("30s"), awaitData.name);
        assertFalse(awaitData.name.contains("event=done"), awaitData.name);
    }

    @Test(description = "WebSocket 固定时长等待不应在 Await 节点标题展示消息过滤条件")
    public void shouldHideWebSocketMessageFilterInFixedDurationAwaitNodeTitle() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode awaitNode = newNode("Await", NodeType.WS_AWAIT);
        JMeterTreeNode awaitData = (JMeterTreeNode) awaitNode.getUserObject();
        awaitData.webSocketPerformanceData = new com.laker.postman.panel.performance.model.WebSocketPerformanceData();
        awaitData.webSocketPerformanceData.completionMode = com.laker.postman.panel.performance.model.WebSocketPerformanceData.CompletionMode.FIXED_DURATION;
        awaitData.webSocketPerformanceData.holdConnectionMs = 30000;
        awaitData.webSocketPerformanceData.messageFilter = "done";
        context.treeModel.insertNodeInto(awaitNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertTrue(awaitData.name.contains("30s"), awaitData.name);
        assertFalse(awaitData.name.contains("contains=done"), awaitData.name);
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

    @Test(description = "WebSocket 同步结构时不应把 await 节点下的断言搬回请求节点")
    public void shouldKeepWebSocketAwaitAssertionsWhenSyncingStructure() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode awaitNode = newNode("Await", NodeType.WS_AWAIT);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        awaitNode.add(assertionNode);
        context.treeModel.insertNodeInto(awaitNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertSame(assertionNode.getParent(), awaitNode);
        assertEquals(childTypesOf(awaitNode), List.of(NodeType.ASSERTION));
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

    @Test(description = "WebSocket 切回 HTTP 时应清理 Loop 中的步骤，并恢复嵌套 Await 断言")
    public void shouldRemoveWebSocketLoopAndRestoreNestedAwaitAssertionsWhenSwitchingBackToHttp() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode loopNode = newLoopNode(2);
        DefaultMutableTreeNode awaitNode = newNode("Await", NodeType.WS_AWAIT);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        awaitNode.add(assertionNode);
        loopNode.add(newNode("Send", NodeType.WS_SEND));
        loopNode.add(awaitNode);
        context.treeModel.insertNodeInto(loopNode, context.requestNode, context.requestNode.getChildCount());

        context.requestData.httpRequestItem.setProtocol(RequestItemProtocolEnum.HTTP);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.ASSERTION));
        assertSame(context.requestNode.getChildAt(0), assertionNode);
        assertNull(findChild(context.requestNode, NodeType.LOOP));
    }

    @Test(description = "WebSocket Loop 应作为步骤容器，便于在其中继续添加 Send/Await/Timer")
    public void shouldResolveWebSocketLoopAsStepParent() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode loopNode = newLoopNode(2);
        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        loopNode.add(sendNode);
        context.treeModel.insertNodeInto(loopNode, context.requestNode, context.requestNode.getChildCount());

        assertSame(context.treeSupport.resolveWebSocketStepParent(loopNode), loopNode);
        assertSame(context.treeSupport.resolveWebSocketStepParent(sendNode), loopNode);
        assertFalse(context.treeSupport.isRequestContainerLoop(loopNode));
    }

    @Test(description = "WebSocket Send 重复次数应标注为每轮次数，避免和外层 Loop 总次数混淆")
    public void shouldShowWebSocketSendRepeatCountAsPerLoopCount() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        JMeterTreeNode sendData = (JMeterTreeNode) sendNode.getUserObject();
        sendData.webSocketPerformanceData = new WebSocketPerformanceData();
        sendData.webSocketPerformanceData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
        sendData.webSocketPerformanceData.sendCount = 3;
        context.treeModel.insertNodeInto(sendNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertTrue(sendData.name.contains("每轮 3 次") || sendData.name.contains("Per loop 3x"), sendData.name);
        assertFalse(sendData.name.contains(" | 3x | "), sendData.name);
    }

    @Test(description = "线程组下的 Loop 应作为请求容器，供 HTTP/SSE/WS 请求复用")
    public void shouldIdentifyLoopUnderThreadGroupAsRequestContainer() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode loopNode = newLoopNode(3);
        root.add(groupNode);
        groupNode.add(loopNode);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));

        assertTrue(treeSupport.isRequestContainerLoop(loopNode));
    }

    @Test(description = "复制请求节点应复制完整子树并生成新的请求 id")
    public void shouldCopyRequestSubtreeWithIndependentRequestId() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.requestData.httpRequestItem.setId("original-request");
        context.requestData.httpRequestItem.setUrl("ws://localhost:8080/ws");
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        JMeterTreeNode sendData = (JMeterTreeNode) sendNode.getUserObject();
        sendData.webSocketPerformanceData = new WebSocketPerformanceData();
        sendData.webSocketPerformanceData.customSendBody = "hello";
        context.treeModel.insertNodeInto(sendNode, context.requestNode, context.requestNode.getChildCount());

        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(context.requestNode));

        assertEquals(copied.size(), 1);
        JMeterTreeNode copiedRequestData = (JMeterTreeNode) copied.get(0).getUserObject();
        assertNotSame(copiedRequestData.httpRequestItem, context.requestData.httpRequestItem);
        assertNotEquals(copiedRequestData.httpRequestItem.getId(), context.requestData.httpRequestItem.getId());
        assertEquals(copiedRequestData.httpRequestItem.getUrl(), "ws://localhost:8080/ws");
        assertEquals(childTypesOf(copied.get(0)), List.of(NodeType.WS_CONNECT, NodeType.WS_SEND));
        JMeterTreeNode copiedSendData = (JMeterTreeNode) ((DefaultMutableTreeNode) copied.get(0).getChildAt(1)).getUserObject();
        assertNotSame(copiedSendData.webSocketPerformanceData, sendData.webSocketPerformanceData);
        assertEquals(copiedSendData.webSocketPerformanceData.customSendBody, "hello");
    }

    @Test(description = "多选复制时应过滤已包含在父节点子树中的重复子节点")
    public void shouldCopyOnlyTopLevelNodesFromMultiSelection() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode loopNode = newLoopNode(3);
        DefaultMutableTreeNode nestedRequest = newRequestNode("nested", "Nested");
        DefaultMutableTreeNode siblingRequest = newRequestNode("sibling", "Sibling");
        root.add(groupNode);
        groupNode.add(loopNode);
        loopNode.add(nestedRequest);
        groupNode.add(siblingRequest);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));

        List<DefaultMutableTreeNode> copied = treeSupport.copyNodes(paths(loopNode, nestedRequest, siblingRequest));

        assertEquals(copied.size(), 2);
        assertEquals(nodeType(copied.get(0)), NodeType.LOOP);
        assertEquals(nodeType(copied.get(1)), NodeType.REQUEST);
        assertEquals(childTypesOf(copied.get(0)), List.of(NodeType.REQUEST));
    }

    @Test(description = "粘贴请求到另一个请求上时应作为同级插入到目标之后")
    public void shouldPasteRequestAfterSelectedSiblingWhenTargetCannotContainRequest() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode firstRequest = newRequestNode("first", "First");
        DefaultMutableTreeNode secondRequest = newRequestNode("second", "Second");
        root.add(groupNode);
        groupNode.add(firstRequest);
        groupNode.add(secondRequest);
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(treeModel);
        List<DefaultMutableTreeNode> copied = treeSupport.copyNodes(paths(firstRequest));

        List<DefaultMutableTreeNode> pasted = treeSupport.pasteNodes(new JTree(treeModel), secondRequest, copied);

        assertEquals(pasted.size(), 1);
        assertSame(groupNode.getChildAt(2), pasted.get(0));
        JMeterTreeNode pastedData = (JMeterTreeNode) pasted.get(0).getUserObject();
        assertNotEquals(pastedData.httpRequestItem.getId(), "first");
        assertNotEquals(pastedData.httpRequestItem.getId(), ((JMeterTreeNode) copied.get(0).getUserObject()).httpRequestItem.getId());
    }

    @Test(description = "粘贴 WebSocket 步骤到 WebSocket 请求上时应插入步骤并保留步骤配置")
    public void shouldPasteWebSocketStepIntoWebSocketRequest() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        JMeterTreeNode sendData = (JMeterTreeNode) sendNode.getUserObject();
        sendData.webSocketPerformanceData = new WebSocketPerformanceData();
        sendData.webSocketPerformanceData.customSendBody = "payload";
        context.treeModel.insertNodeInto(sendNode, context.requestNode, context.requestNode.getChildCount());
        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(sendNode));

        List<DefaultMutableTreeNode> pasted = context.treeSupport.pasteNodes(new JTree(context.treeModel), context.requestNode, copied);

        assertEquals(pasted.size(), 1);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT, NodeType.WS_SEND, NodeType.WS_SEND));
        JMeterTreeNode pastedData = (JMeterTreeNode) pasted.get(0).getUserObject();
        assertNotSame(pastedData.webSocketPerformanceData, sendData.webSocketPerformanceData);
        assertEquals(pastedData.webSocketPerformanceData.customSendBody, "payload");
    }

    @Test(description = "SSE 固定阶段节点也应支持单独复制")
    public void shouldCopySseStageNodesIndividually() {
        TestContext context = newTestContext(RequestItemProtocolEnum.SSE);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.SSE_CONNECT);
        DefaultMutableTreeNode awaitNode = findChild(context.requestNode, NodeType.SSE_AWAIT);

        assertTrue(context.treeSupport.hasCopyableNodes(paths(connectNode)));
        assertTrue(context.treeSupport.hasCopyableNodes(paths(awaitNode)));
        assertEquals(context.treeSupport.copyNodes(paths(connectNode, awaitNode)).size(), 2);
    }

    @Test(description = "粘贴 SSE 阶段节点到 SSE 请求上时应插入阶段节点")
    public void shouldPasteSseStageNodeIntoSseRequest() {
        TestContext context = newTestContext(RequestItemProtocolEnum.SSE);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode awaitNode = findChild(context.requestNode, NodeType.SSE_AWAIT);
        JMeterTreeNode awaitData = (JMeterTreeNode) awaitNode.getUserObject();
        awaitData.ssePerformanceData = new SsePerformanceData();
        awaitData.ssePerformanceData.messageFilter = "ready";
        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(awaitNode));

        List<DefaultMutableTreeNode> pasted = context.treeSupport.pasteNodes(new JTree(context.treeModel), context.requestNode, copied);

        assertEquals(pasted.size(), 1);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.SSE_CONNECT, NodeType.SSE_AWAIT, NodeType.SSE_AWAIT));
        JMeterTreeNode pastedData = (JMeterTreeNode) pasted.get(0).getUserObject();
        assertNotSame(pastedData.ssePerformanceData, awaitData.ssePerformanceData);
        assertEquals(pastedData.ssePerformanceData.messageFilter, "ready");
    }

    @Test(description = "WebSocket Connect 节点应支持单独复制")
    public void shouldCopyWebSocketConnectNodeIndividually() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.WS_CONNECT);

        assertTrue(context.treeSupport.hasCopyableNodes(paths(connectNode)));
        assertEquals(context.treeSupport.copyNodes(paths(connectNode)).size(), 1);
    }

    @Test(description = "粘贴 WebSocket Connect 到 WebSocket 请求上时应插入连接节点")
    public void shouldPasteWebSocketConnectIntoWebSocketRequest() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.WS_CONNECT);
        JMeterTreeNode connectData = (JMeterTreeNode) connectNode.getUserObject();
        connectData.webSocketPerformanceData = new WebSocketPerformanceData();
        connectData.webSocketPerformanceData.connectTimeoutMs = 15000;
        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(connectNode));

        List<DefaultMutableTreeNode> pasted = context.treeSupport.pasteNodes(new JTree(context.treeModel), context.requestNode, copied);

        assertEquals(pasted.size(), 1);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT, NodeType.WS_CONNECT));
        JMeterTreeNode pastedData = (JMeterTreeNode) pasted.get(0).getUserObject();
        assertNotSame(pastedData.webSocketPerformanceData, connectData.webSocketPerformanceData);
        assertEquals(pastedData.webSocketPerformanceData.connectTimeoutMs, 15000);
    }

    @Test(description = "SSE Connect、SSE Receive、WebSocket Connect 阶段节点应支持删除")
    public void shouldDeleteProtocolStageNodes() {
        TestContext sseContext = newTestContext(RequestItemProtocolEnum.SSE);
        sseContext.treeSupport.syncRequestStructure(sseContext.requestNode, sseContext.requestData);
        DefaultMutableTreeNode sseConnectNode = findChild(sseContext.requestNode, NodeType.SSE_CONNECT);
        DefaultMutableTreeNode sseAwaitNode = findChild(sseContext.requestNode, NodeType.SSE_AWAIT);

        assertTrue(sseContext.treeSupport.hasDeletableNodes(paths(sseConnectNode, sseAwaitNode)));
        List<DefaultMutableTreeNode> deletedSseNodes = sseContext.treeSupport.deleteNodes(paths(sseConnectNode, sseAwaitNode));

        assertEquals(deletedSseNodes.size(), 2);
        assertEquals(childTypesOf(sseContext.requestNode), List.of());

        TestContext wsContext = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        wsContext.treeSupport.syncRequestStructure(wsContext.requestNode, wsContext.requestData);
        DefaultMutableTreeNode wsConnectNode = findChild(wsContext.requestNode, NodeType.WS_CONNECT);

        assertTrue(wsContext.treeSupport.hasDeletableNodes(paths(wsConnectNode)));
        List<DefaultMutableTreeNode> deletedWsNodes = wsContext.treeSupport.deleteNodes(paths(wsConnectNode));

        assertEquals(deletedWsNodes.size(), 1);
        assertEquals(childTypesOf(wsContext.requestNode), List.of());
    }

    @Test(description = "删除多选节点时应过滤父节点子树内的重复子节点")
    public void shouldDeleteOnlyTopLevelNodesFromMultiSelection() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode requestNode = newRequestNode("request", "Request");
        DefaultMutableTreeNode timerNode = newNode("Timer", NodeType.TIMER);
        root.add(groupNode);
        groupNode.add(requestNode);
        requestNode.add(timerNode);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));

        List<DefaultMutableTreeNode> deletedNodes = treeSupport.deleteNodes(paths(root, requestNode, timerNode));

        assertEquals(deletedNodes.size(), 1);
        assertSame(deletedNodes.get(0), requestNode);
        assertEquals(childTypesOf(groupNode), List.of());
        assertFalse(treeSupport.hasDeletableNodes(paths(root)));
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

    private static DefaultMutableTreeNode newRequestNode(String id, String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        return new DefaultMutableTreeNode(new JMeterTreeNode(name, NodeType.REQUEST, item));
    }

    private static DefaultMutableTreeNode newLoopNode(int iterations) {
        JMeterTreeNode loopData = new JMeterTreeNode("Loop", NodeType.LOOP);
        loopData.loopData = new LoopData();
        loopData.loopData.iterations = iterations;
        return new DefaultMutableTreeNode(loopData);
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

    private static TreePath[] paths(DefaultMutableTreeNode... nodes) {
        TreePath[] paths = new TreePath[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            paths[i] = new TreePath(nodes[i].getPath());
        }
        return paths;
    }

    private static NodeType nodeType(DefaultMutableTreeNode node) {
        return ((JMeterTreeNode) node.getUserObject()).type;
    }

    private record TestContext(
            PerformanceTreeSupport treeSupport,
            DefaultTreeModel treeModel,
            DefaultMutableTreeNode requestNode,
            JMeterTreeNode requestData
    ) {
    }
}

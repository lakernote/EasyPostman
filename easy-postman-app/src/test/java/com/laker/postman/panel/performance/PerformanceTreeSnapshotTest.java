package com.laker.postman.panel.performance;

import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;

public class PerformanceTreeSnapshotTest {

    @Test(description = "执行快照应深拷贝树和节点数据，并保留原请求 id")
    public void shouldDeepCopyExecutionTreeWithoutChangingRequestIds() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-id");
        item.setName("Original");
        item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);

        PerformanceTreeNode requestData = new PerformanceTreeNode("Original", NodeType.REQUEST, item);
        requestData.webSocketPerformanceData = new WebSocketPerformanceData();
        requestData.webSocketPerformanceData.connectTimeoutMs = 15000;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);
        root.add(requestNode);

        DefaultMutableTreeNode snapshotRoot = PerformanceTreeSnapshot.copy(root);
        PerformanceTreeNode snapshotRequestData =
                (PerformanceTreeNode) ((DefaultMutableTreeNode) snapshotRoot.getChildAt(0)).getUserObject();

        item.setName("Mutated");
        requestData.webSocketPerformanceData.connectTimeoutMs = 1;

        assertNotSame(snapshotRoot, root);
        assertNotSame(snapshotRequestData, requestData);
        assertNotSame(snapshotRequestData.httpRequestItem, requestData.httpRequestItem);
        assertNotSame(snapshotRequestData.webSocketPerformanceData, requestData.webSocketPerformanceData);
        assertEquals(snapshotRequestData.httpRequestItem.getId(), "request-id");
        assertEquals(snapshotRequestData.httpRequestItem.getName(), "Original");
        assertEquals(snapshotRequestData.webSocketPerformanceData.connectTimeoutMs, 15000);
    }

    @Test(description = "执行快照应保留已合并的集合继承快照标记，避免运行前再次合并")
    public void shouldPreserveRequestInheritanceSnapshotFlag() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("snapshot-request-id");
        item.setName("Snapshot Request");

        PerformanceTreeNode requestData = new PerformanceTreeNode("Snapshot Request", NodeType.REQUEST, item);
        requestData.requestInheritanceSnapshot = true;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("Plan", NodeType.ROOT));
        root.add(new DefaultMutableTreeNode(requestData));

        DefaultMutableTreeNode snapshotRoot = PerformanceTreeSnapshot.copy(root);
        PerformanceTreeNode snapshotRequestData =
                (PerformanceTreeNode) ((DefaultMutableTreeNode) snapshotRoot.getChildAt(0)).getUserObject();

        assertEquals(snapshotRequestData.requestInheritanceSnapshot, true);
    }

    @Test(description = "执行快照应保留仅存在于请求快照中的旧请求数据")
    public void shouldCopyRequestSnapshotWhenRequestItemIsAbsent() {
        PerformanceRequestSnapshot requestSnapshot = PerformanceRequestSnapshot.builder()
                .id("snapshot-id")
                .name("Snapshot Request")
                .url("https://example.com/api")
                .method("POST")
                .protocol(PerformanceProtocol.HTTP)
                .body("payload")
                .build();
        PerformanceTreeNode requestData = new PerformanceTreeNode("Snapshot Request", NodeType.REQUEST);
        requestData.requestSnapshot = requestSnapshot;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("Plan", NodeType.ROOT));
        root.add(new DefaultMutableTreeNode(requestData));

        DefaultMutableTreeNode snapshotRoot = PerformanceTreeSnapshot.copy(root);
        PerformanceTreeNode snapshotRequestData =
                (PerformanceTreeNode) ((DefaultMutableTreeNode) snapshotRoot.getChildAt(0)).getUserObject();

        assertNotSame(snapshotRequestData.requestSnapshot, requestSnapshot);
        assertEquals(snapshotRequestData.requestSnapshot.getId(), "snapshot-id");
        assertEquals(snapshotRequestData.requestSnapshot.getUrl(), "https://example.com/api");
        assertEquals(snapshotRequestData.requestSnapshot.getBody(), "payload");
    }

    @Test(description = "粘贴请求快照节点时也应刷新请求 id，避免重复请求标识")
    public void copyForPasteShouldRegenerateRequestSnapshotId() {
        PerformanceRequestSnapshot requestSnapshot = PerformanceRequestSnapshot.builder()
                .id("original-id")
                .name("Snapshot Request")
                .url("https://example.com/api")
                .build();
        PerformanceTreeNode requestData = new PerformanceTreeNode("Snapshot Request", NodeType.REQUEST);
        requestData.requestSnapshot = requestSnapshot;
        DefaultMutableTreeNode source = new DefaultMutableTreeNode(requestData);

        DefaultMutableTreeNode pastedNode = PerformanceTreeSnapshot.copyForPaste(source);
        PerformanceTreeNode pastedData = (PerformanceTreeNode) pastedNode.getUserObject();

        assertNotSame(pastedData.requestSnapshot, requestSnapshot);
        assertNotEquals(pastedData.requestSnapshot.getId(), "original-id");
    }

    @Test(description = "执行快照应深拷贝条件控制器配置")
    public void shouldDeepCopyConditionData() {
        ConditionData conditionData = new ConditionData();
        conditionData.expression = "{{enabled}} == true";
        PerformanceTreeNode conditionNodeData = new PerformanceTreeNode("Condition", NodeType.CONDITION);
        conditionNodeData.conditionData = conditionData;
        DefaultMutableTreeNode source = new DefaultMutableTreeNode(conditionNodeData);

        DefaultMutableTreeNode copiedNode = PerformanceTreeSnapshot.copy(source);
        PerformanceTreeNode copiedData = (PerformanceTreeNode) copiedNode.getUserObject();

        conditionData.expression = "false";

        assertNotSame(copiedData.conditionData, conditionData);
        assertEquals(copiedData.conditionData.expression, "{{enabled}} == true");
    }
}

package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

public class PerformanceTreeSnapshotTest {

    @Test(description = "执行快照应深拷贝树和节点数据，并保留原请求 id")
    public void shouldDeepCopyExecutionTreeWithoutChangingRequestIds() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-id");
        item.setName("Original");
        item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);

        JMeterTreeNode requestData = new JMeterTreeNode("Original", NodeType.REQUEST, item);
        requestData.webSocketPerformanceData = new WebSocketPerformanceData();
        requestData.webSocketPerformanceData.connectTimeoutMs = 15000;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);
        root.add(requestNode);

        DefaultMutableTreeNode snapshotRoot = PerformanceTreeSnapshot.copy(root);
        JMeterTreeNode snapshotRequestData =
                (JMeterTreeNode) ((DefaultMutableTreeNode) snapshotRoot.getChildAt(0)).getUserObject();

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

        JMeterTreeNode requestData = new JMeterTreeNode("Snapshot Request", NodeType.REQUEST, item);
        requestData.requestInheritanceSnapshot = true;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        root.add(new DefaultMutableTreeNode(requestData));

        DefaultMutableTreeNode snapshotRoot = PerformanceTreeSnapshot.copy(root);
        JMeterTreeNode snapshotRequestData =
                (JMeterTreeNode) ((DefaultMutableTreeNode) snapshotRoot.getChildAt(0)).getUserObject();

        assertEquals(snapshotRequestData.requestInheritanceSnapshot, true);
    }
}

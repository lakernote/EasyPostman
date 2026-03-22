package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.service.PerformancePersistenceService;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

public class PerformanceRequestSyncSupportTest {

    @Test(description = "syncRequestItem 应更新树中匹配请求，并同步当前编辑器内容")
    public void shouldSyncMatchingRequestAndRefreshCurrentEditor() {
        HttpRequestItem oldItem = requestItem("req-1", "Old Request", "https://old.example.com");
        TestContext context = newTestContext(oldItem);
        List<String> syncEvents = new ArrayList<>();
        AtomicReference<HttpRequestItem> switchedItem = new AtomicReference<>();
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                new PerformancePersistenceService(),
                (node, data) -> syncEvents.add(data.httpRequestItem.getId())
        );
        HttpRequestItem latestItem = requestItem("req-1", "New Request", "https://new.example.com");

        support.syncRequestItem(context.rootNode, latestItem, context.requestNode, switchedItem::set);

        JMeterTreeNode requestData = (JMeterTreeNode) context.requestNode.getUserObject();
        assertSame(requestData.httpRequestItem, latestItem);
        assertEquals(requestData.name, "New Request");
        assertEquals(syncEvents, List.of("req-1"));
        assertSame(switchedItem.get(), latestItem);
    }

    @Test(description = "syncRequestItem 遇到空请求或空 id 时应直接忽略")
    public void shouldIgnoreRequestWithoutId() {
        HttpRequestItem oldItem = requestItem("req-1", "Old Request", "https://old.example.com");
        TestContext context = newTestContext(oldItem);
        List<String> syncEvents = new ArrayList<>();
        AtomicReference<HttpRequestItem> switchedItem = new AtomicReference<>();
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                new PerformancePersistenceService(),
                (node, data) -> syncEvents.add(data.httpRequestItem.getId())
        );
        HttpRequestItem invalidItem = new HttpRequestItem();
        invalidItem.setName("No Id");

        support.syncRequestItem(context.rootNode, invalidItem, context.requestNode, switchedItem::set);

        JMeterTreeNode requestData = (JMeterTreeNode) context.requestNode.getUserObject();
        assertSame(requestData.httpRequestItem, oldItem);
        assertEquals(requestData.name, "Old Request");
        assertEquals(syncEvents.size(), 0);
        assertNull(switchedItem.get());
    }

    private static TestContext newTestContext(HttpRequestItem item) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new JMeterTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new JMeterTreeNode("Group", NodeType.THREAD_GROUP));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new JMeterTreeNode(item.getName(), NodeType.REQUEST, item));
        rootNode.add(groupNode);
        groupNode.add(requestNode);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        return new TestContext(rootNode, requestNode, treeModel);
    }

    private static HttpRequestItem requestItem(String id, String name, String url) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        item.setUrl(url);
        return item;
    }

    private record TestContext(
            DefaultMutableTreeNode rootNode,
            DefaultMutableTreeNode requestNode,
            DefaultTreeModel treeModel
    ) {
    }
}

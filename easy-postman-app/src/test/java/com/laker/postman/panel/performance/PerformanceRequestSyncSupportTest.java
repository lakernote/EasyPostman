package com.laker.postman.panel.performance;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.performance.plan.PerformanceRequestSnapshotMapper;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionTreeDocumentMapper;
import com.laker.postman.service.collections.CollectionDocumentRegistry;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.service.collections.CollectionTreeRootRegistry;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

public class PerformanceRequestSyncSupportTest extends AbstractSwingUiTest {

    @AfterMethod
    public void clearCollectionRootAndRequestScope() {
        CollectionTreeRootRegistry.clear();
        CollectionDocumentRegistry.registerDocumentSupplier(com.laker.postman.collection.model.CollectionDocument::empty);
        RequestExecutionContext.clearCurrentScope();
    }

    @Test(description = "syncRequestItem 应更新树中匹配请求，并同步当前编辑器内容")
    public void shouldSyncMatchingRequestAndRefreshCurrentEditor() {
        HttpRequestItem oldItem = requestItem("req-1", "Old Request", "https://old.example.com");
        TestContext context = newTestContext(oldItem);
        List<String> syncEvents = new ArrayList<>();
        AtomicReference<HttpRequestItem> switchedItem = new AtomicReference<>();
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                (node, data) -> syncEvents.add(data.httpRequestItem.getId())
        );
        HttpRequestItem latestItem = requestItem("req-1", "New Request", "https://new.example.com");

        support.syncRequestItem(context.rootNode, latestItem, context.requestNode, switchedItem::set);

        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        assertNotSame(requestData.httpRequestItem, latestItem);
        assertEquals(requestData.httpRequestItem.getUrl(), "https://new.example.com");
        assertEquals(requestData.name, "New Request");
        assertEquals(syncEvents, List.of("req-1"));
        assertSame(switchedItem.get(), requestData.httpRequestItem);
    }

    @Test(description = "syncRequestItem 应刷新当前请求的分组变量作用域，避免 tooltip 使用旧快照")
    public void shouldRefreshCurrentRequestScopeWhenSyncingMatchingRequest() {
        HttpRequestItem oldItem = requestItem("req-1", "Old Request", "https://old.example.com?test={{testname}}");
        TestContext context = newTestContext(oldItem);
        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        requestData.requestExecutionScope = RequestExecutionScope.fromGroupVariables(Map.of("testname", "333"));
        requestData.requestSnapshot = PerformanceRequestSnapshotMapper.fromHttpRequestItem(
                oldItem,
                requestData.requestExecutionScope
        );
        RequestExecutionContext.setCurrentScope(requestData.requestExecutionScope);

        HttpRequestItem latestItem = requestItem("req-1", "New Request", "https://new.example.com?test={{testname}}");
        registerCollectionRequest(latestItem, "888");
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                (node, data) -> {
                }
        );

        support.syncRequestItem(context.rootNode, latestItem, context.requestNode, ignored -> {
        });

        assertEquals(requestData.requestExecutionScope.getGroupVariable("testname"), "888");
        assertEquals(requestData.requestSnapshot.getExecutionScope().getGroupVariable("testname"), "888");
        assertEquals(RequestExecutionContext.getCurrentScope().getGroupVariable("testname"), "888");
    }

    @Test(description = "刷新 collection 请求时应同步执行快照，避免编辑器显示新 URL 但实际发送旧 URL")
    public void shouldRefreshRequestSnapshotWhenRefreshingFromCollections() {
        HttpRequestItem oldItem = requestItem("req-1", "GET Example", "https://httpbin.org/get?test={{testname}}");
        TestContext context = newTestContext(oldItem);
        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        requestData.requestExecutionScope = RequestExecutionScope.fromGroupVariables(Map.of("testname", "333"));
        requestData.requestSnapshot = PerformanceRequestSnapshotMapper.fromHttpRequestItem(
                oldItem,
                requestData.requestExecutionScope
        );

        HttpRequestItem latestItem = requestItem("req-1", "GET Example", "https://httpbingo.org/get?test={{testname}}");
        registerCollectionRequest(latestItem, "888");
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                (node, data) -> {
                }
        );

        support.refreshRequestsFromCollections(
                context.requestNode,
                ignored -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        );

        assertEquals(requestData.httpRequestItem.getUrl(), "https://httpbingo.org/get?test={{testname}}");
        assertEquals(requestData.requestSnapshot.getUrl(), "https://httpbingo.org/get?test={{testname}}");
        assertEquals(requestData.requestSnapshot.getExecutionScope().getGroupVariable("testname"), "888");
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
                (node, data) -> syncEvents.add(data.httpRequestItem.getId())
        );
        HttpRequestItem invalidItem = new HttpRequestItem();
        invalidItem.setName("No Id");

        support.syncRequestItem(context.rootNode, invalidItem, context.requestNode, switchedItem::set);

        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        assertSame(requestData.httpRequestItem, oldItem);
        assertEquals(requestData.name, "Old Request");
        assertEquals(syncEvents.size(), 0);
        assertNull(switchedItem.get());
    }

    private static TestContext newTestContext(HttpRequestItem item) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new PerformanceTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new PerformanceTreeNode("Group", NodeType.THREAD_GROUP));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new PerformanceTreeNode(item.getName(), NodeType.REQUEST, item));
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

    private static void registerCollectionRequest(HttpRequestItem item, String variableValue) {
        RequestGroup group = new RequestGroup("Group");
        group.setVariables(List.of(new Variable(true, "testname", variableValue)));
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(item));
        rootNode.add(groupNode);
        CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);
        CollectionDocumentRegistry.registerDocumentSupplier(() -> SwingCollectionTreeDocumentMapper.fromRoot(rootNode));
    }

    private record TestContext(
            DefaultMutableTreeNode rootNode,
            DefaultMutableTreeNode requestNode,
            DefaultTreeModel treeModel
    ) {
    }
}

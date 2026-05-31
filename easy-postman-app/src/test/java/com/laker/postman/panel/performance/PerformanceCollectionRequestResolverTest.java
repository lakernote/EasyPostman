package com.laker.postman.panel.performance;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.service.collections.CollectionTreeRootRegistry;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static com.laker.postman.service.collections.CollectionTreeNodeTypes.REQUEST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

public class PerformanceCollectionRequestResolverTest {

    @Test(description = "应通过已注册的 Collection 树根节点按 ID 查找请求并返回深拷贝")
    public void shouldFindRequestItemFromRegisteredCollectionTreeRoot() {
        HttpRequestItem requestItem = new HttpRequestItem();
        requestItem.setId("req-registered");
        requestItem.setName("registered");
        requestItem.setUrl("https://example.com/registered");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        rootNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, requestItem}));

        try {
            CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);

            HttpRequestItem result = new PerformanceCollectionRequestResolver().findRequestItemById("req-registered");

            assertNotSame(result, requestItem);
            assertEquals(result.getId(), requestItem.getId());
            assertEquals(result.getUrl(), requestItem.getUrl());
        } finally {
            CollectionTreeRootRegistry.clear();
        }
    }
}

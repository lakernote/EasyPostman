package com.laker.postman.panel.collections.tree.adapter;

import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class SwingCollectionTreeQueriesTest {

    @Test
    public void shouldResolveRestorableRequestsInOriginalOrder() {
        HttpRequestItem persistedRequest = request("persisted", "Persisted");
        HttpRequestItem treeResolvedRequest = request("persisted", "Resolved");
        HttpRequestItem missingRequest = request("missing", "Missing");
        HttpRequestItem newRequest = request("new", "");

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        rootNode.add(CollectionTreeNodes.requestNode(treeResolvedRequest));

        List<HttpRequestItem> restorable = SwingCollectionTreeQueries.resolveRestorableOpenedRequests(
                List.of(persistedRequest, missingRequest, newRequest),
                rootNode
        );

        assertEquals(restorable.size(), 2);
        assertSame(restorable.get(0), treeResolvedRequest);
        assertSame(restorable.get(1), newRequest);
    }

    private HttpRequestItem request(String id, String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        return item;
    }
}

package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class RequestCollectionsServiceTest {

    @Test
    public void shouldBuildRestorableRequestsInOriginalOrder() {
        HttpRequestItem persistedRequest = request("persisted", "Persisted");
        HttpRequestItem treeResolvedRequest = request("persisted", "Resolved");
        HttpRequestItem missingRequest = request("missing", "Missing");
        HttpRequestItem newRequest = request("new", "");

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        rootNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, treeResolvedRequest}));

        List<HttpRequestItem> restorable = RequestCollectionsService.buildRestorableOpenedRequests(
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

package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Optional;

import static com.laker.postman.service.collections.CollectionTreeNodeTypes.REQUEST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

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

    @Test
    public void activeTreeNodeRepositoryShouldReadRegisteredRootNode() {
        HttpRequestItem request = request("persisted", "Persisted");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{REQUEST, request});
        rootNode.add(requestNode);

        try {
            CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);
            Optional<DefaultMutableTreeNode> result =
                    new ActiveCollectionTreeNodeRepository().findNodeByRequestId("persisted");

            assertTrue(result.isPresent());
            assertSame(result.get(), requestNode);
        } finally {
            CollectionTreeRootRegistry.clear();
        }
    }


    private HttpRequestItem request(String id, String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        return item;
    }
}

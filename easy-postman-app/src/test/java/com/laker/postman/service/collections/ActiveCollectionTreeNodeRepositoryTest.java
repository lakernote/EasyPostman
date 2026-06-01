package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Optional;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ActiveCollectionTreeNodeRepositoryTest {

    @Test
    public void shouldReadRegisteredRootNode() {
        HttpRequestItem request = request("persisted", "Persisted");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(request);
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

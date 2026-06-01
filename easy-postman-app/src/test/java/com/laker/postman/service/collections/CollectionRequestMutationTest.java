package com.laker.postman.service.collections;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class CollectionRequestMutationTest {

    @Test
    public void shouldUpdateExistingRequestPayloadAndPreservePersistedFields() {
        SavedResponse savedResponse = new SavedResponse();
        savedResponse.setId("response-1");
        HttpRequestItem persisted = new HttpRequestItem();
        persisted.setId("request-1");
        persisted.setName("Persisted");
        persisted.setResponse(List.of(savedResponse));
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(persisted);
        DefaultMutableTreeNode root = rootWith(requestNode);

        HttpRequestItem edited = new HttpRequestItem();
        edited.setId("request-1");
        edited.setName("Edited");
        edited.setUrl("https://api.example.com");

        CollectionRequestMutation.Result result = CollectionRequestMutation
                .updateExistingRequest(root, edited)
                .orElseThrow();

        assertSame(result.requestNode(), requestNode);
        assertSame(result.updatedItem(), edited);
        assertSame(CollectionTreeNodes.request(requestNode).orElseThrow(), edited);
        assertEquals(edited.getName(), "Persisted");
        assertSame(edited.getResponse(), persisted.getResponse());
    }

    @Test
    public void shouldReturnEmptyWhenRequestCannotBeResolved() {
        HttpRequestItem edited = new HttpRequestItem();
        edited.setId("missing");

        assertTrue(CollectionRequestMutation.updateExistingRequest(rootWith(), edited).isEmpty());
    }

    private DefaultMutableTreeNode rootWith(DefaultMutableTreeNode... children) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(new RequestGroup("Group"));
        root.add(groupNode);
        for (DefaultMutableTreeNode child : children) {
            groupNode.add(child);
        }
        return root;
    }
}

package com.laker.postman.service.collections;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;


import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class CollectionTreeNodesTest {

    @Test
    public void shouldCreateAndReadTypedCollectionTreeNodes() {
        RequestGroup group = new RequestGroup("Group");
        HttpRequestItem request = new HttpRequestItem();
        SavedResponse savedResponse = new SavedResponse();

        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(request);
        DefaultMutableTreeNode responseNode = CollectionTreeNodes.savedResponseNode(savedResponse);

        assertTrue(CollectionTreeNodes.isGroup(groupNode));
        assertTrue(CollectionTreeNodes.isRequest(requestNode));
        assertTrue(CollectionTreeNodes.isSavedResponse(responseNode));
        assertSame(CollectionTreeNodes.group(groupNode).orElseThrow(), group);
        assertSame(CollectionTreeNodes.request(requestNode).orElseThrow(), request);
        assertSame(CollectionTreeNodes.savedResponse(responseNode).orElseThrow(), savedResponse);
        assertSame(CollectionTreeNodes.groupPayload(groupNode.getUserObject()).orElseThrow(), group);
    }

    @Test
    public void shouldIgnoreMalformedUserObjects() {
        DefaultMutableTreeNode malformedNode = new DefaultMutableTreeNode(new Object[]{
                CollectionTreeNodeTypes.REQUEST,
                "not-a-request"
        });

        assertFalse(CollectionTreeNodes.isRequest(malformedNode));
        assertTrue(CollectionTreeNodes.request(malformedNode).isEmpty());
    }

    @Test
    public void shouldReplaceRequestPayloadThroughAccessor() {
        HttpRequestItem original = new HttpRequestItem();
        original.setId("original");
        HttpRequestItem replacement = new HttpRequestItem();
        replacement.setId("replacement");
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(original);

        CollectionTreeNodes.setRequest(requestNode, replacement);

        assertSame(CollectionTreeNodes.request(requestNode).orElseThrow(), replacement);
    }
}

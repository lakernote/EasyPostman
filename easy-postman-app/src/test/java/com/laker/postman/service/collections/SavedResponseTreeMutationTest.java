package com.laker.postman.service.collections;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SavedResponseTreeMutationTest {

    @Test
    public void shouldAppendSavedResponseToTreeAndEditorRequest() {
        HttpRequestItem treeItem = new HttpRequestItem();
        treeItem.setId("request-1");
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(treeItem);
        DefaultMutableTreeNode root = rootWith(requestNode);

        HttpRequestItem editorItem = new HttpRequestItem();
        editorItem.setId("request-1");
        SavedResponse savedResponse = new SavedResponse();
        savedResponse.setId("response-1");

        SavedResponseTreeMutation.Result result = SavedResponseTreeMutation
                .appendSavedResponse(root, editorItem, savedResponse)
                .orElseThrow();

        assertSame(result.requestNode(), requestNode);
        assertSame(result.treeRequestItem(), treeItem);
        assertEquals(treeItem.getResponse(), editorItem.getResponse());
        assertEquals(treeItem.getResponse().size(), 1);
        assertSame(treeItem.getResponse().get(0), savedResponse);
        assertEquals(requestNode.getChildCount(), 1);
        assertTrue(CollectionTreeNodes.isSavedResponse((DefaultMutableTreeNode) requestNode.getChildAt(0)));
    }

    @Test
    public void shouldNotDuplicateWhenEditorRequestIsTreePayload() {
        HttpRequestItem treeItem = new HttpRequestItem();
        treeItem.setId("request-1");
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(treeItem);
        DefaultMutableTreeNode root = rootWith(requestNode);
        SavedResponse savedResponse = new SavedResponse();

        SavedResponseTreeMutation.appendSavedResponse(root, treeItem, savedResponse).orElseThrow();

        assertEquals(treeItem.getResponse().size(), 1);
    }

    private DefaultMutableTreeNode rootWith(DefaultMutableTreeNode requestNode) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(new RequestGroup("Group"));
        groupNode.add(requestNode);
        root.add(groupNode);
        return root;
    }
}

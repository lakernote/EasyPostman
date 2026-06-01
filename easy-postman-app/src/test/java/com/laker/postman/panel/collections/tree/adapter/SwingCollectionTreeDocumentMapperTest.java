package com.laker.postman.panel.collections.tree.adapter;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.service.collections.CollectionTreeNodes;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SwingCollectionTreeDocumentMapperTest {

    @Test
    public void shouldReplaceRootChildrenFromCollectionDocument() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        root.add(CollectionTreeNodes.groupNode(new RequestGroup("Old")));
        RequestGroup group = new RequestGroup("Group");
        group.setId("group-1");
        HttpRequestItem request = new HttpRequestItem();
        request.setId("request-1");
        SavedResponse savedResponse = new SavedResponse();
        savedResponse.setId("response-1");
        request.setResponse(List.of(savedResponse));
        CollectionNode groupNode = CollectionNode.group(group);
        groupNode.addChild(CollectionNode.request(request));

        SwingCollectionTreeDocumentMapper.replaceRootChildren(root, new CollectionDocument(List.of(groupNode)));

        assertEquals(root.getChildCount(), 1);
        DefaultMutableTreeNode mappedGroup = (DefaultMutableTreeNode) root.getChildAt(0);
        DefaultMutableTreeNode mappedRequest = (DefaultMutableTreeNode) mappedGroup.getChildAt(0);
        assertTrue(CollectionTreeNodes.isGroup(mappedGroup));
        assertTrue(CollectionTreeNodes.isRequest(mappedRequest));
        assertTrue(CollectionTreeNodes.isSavedResponse((DefaultMutableTreeNode) mappedRequest.getChildAt(0)));
    }

    @Test
    public void shouldMapTreeRootBackToCollectionDocument() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        RequestGroup group = new RequestGroup("Group");
        group.setId("group-1");
        HttpRequestItem request = new HttpRequestItem();
        request.setId("request-1");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(request));
        root.add(groupNode);

        CollectionDocument document = SwingCollectionTreeDocumentMapper.fromRoot(root);

        assertEquals(document.getRoots().size(), 1);
        assertEquals(document.getRoots().get(0).asGroup().getId(), "group-1");
        assertEquals(document.getRoots().get(0).getChildren().get(0).asRequest().getId(), "request-1");
    }
}

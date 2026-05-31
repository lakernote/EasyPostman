package com.laker.postman.common.component.tree;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.service.collections.CollectionTreeNodes;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class CollectionGroupTreeFactoryTest {

    @Test
    public void shouldFilterRequestNodesFromGroupSelectionModel() {
        RequestGroup collection = new RequestGroup();
        collection.setName("Collection");
        RequestGroup folder = new RequestGroup();
        folder.setName("Folder");

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode collectionNode = CollectionTreeNodes.groupNode(collection);
        collectionNode.add(CollectionTreeNodes.requestNode(new HttpRequestItem()));
        collectionNode.add(CollectionTreeNodes.groupNode(folder));
        root.add(collectionNode);

        TreeModel model = CollectionGroupTreeFactory.createModel(new DefaultTreeModel(root));

        assertEquals(model.getChildCount(root), 1);
        assertSame(model.getChild(root, 0), collectionNode);
        assertEquals(model.getChildCount(collectionNode), 1);
        assertSame(model.getChild(collectionNode, 0), collectionNode.getChildAt(1));
    }
}

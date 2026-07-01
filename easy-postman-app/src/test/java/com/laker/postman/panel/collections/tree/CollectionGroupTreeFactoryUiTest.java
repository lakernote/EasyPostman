package com.laker.postman.panel.collections.tree;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class CollectionGroupTreeFactoryUiTest extends AbstractSwingUiTest {

    @Test
    public void shouldNotExpandNestedGroupsByDefault() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode firstCollection = groupNode("Collection A");
        firstCollection.add(groupNode("Folder A"));
        root.add(firstCollection);
        root.add(groupNode("Collection B"));

        SwingUtilities.invokeAndWait(() -> {
            JTree tree = CollectionGroupTreeFactory.createTree(new DefaultTreeModel(root));

            assertEquals(tree.getRowCount(), 2);
            assertEquals(tree.getMinSelectionRow(), 0);
            assertFalse(tree.isExpanded(0));
        });
    }

    private static DefaultMutableTreeNode groupNode(String name) {
        RequestGroup group = new RequestGroup();
        group.setName(name);
        return CollectionTreeNodes.groupNode(group);
    }
}

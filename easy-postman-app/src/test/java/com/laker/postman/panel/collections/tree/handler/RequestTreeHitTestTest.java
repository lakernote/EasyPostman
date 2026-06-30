package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Rectangle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

public class RequestTreeHitTestTest extends AbstractSwingUiTest {

    @Test
    public void rowAtYShouldUseClosestRowWithoutMatchingBlankSpaceBelowRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTree tree = createTree();
            Rectangle firstRowBounds = tree.getRowBounds(0);

            assertEquals(RequestTreeHitTest.rowAtY(tree, firstRowBounds.y + firstRowBounds.height / 2), 0);
            assertEquals(RequestTreeHitTest.rowAtY(tree, firstRowBounds.y + firstRowBounds.height + 20), -1);
        });
    }

    @Test
    public void pathAtShouldResolveFullWidthRowButRejectBlankSpaceBelowRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTree tree = createTree();
            TreePath firstRowPath = tree.getPathForRow(0);
            Rectangle firstRowBounds = tree.getRowBounds(0);

            TreePath resolved = RequestTreeHitTest.pathAt(
                    tree,
                    tree.getWidth() - 4,
                    firstRowBounds.y + firstRowBounds.height / 2
            );

            assertSame(resolved, firstRowPath);
            assertNull(RequestTreeHitTest.pathAt(
                    tree,
                    tree.getWidth() - 4,
                    firstRowBounds.y + firstRowBounds.height + 20
            ));
        });
    }

    private static JTree createTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CollectionTreePanel.ROOT);
        root.add(new DefaultMutableTreeNode("group"));
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(28);
        tree.setSize(300, 120);
        return tree;
    }
}

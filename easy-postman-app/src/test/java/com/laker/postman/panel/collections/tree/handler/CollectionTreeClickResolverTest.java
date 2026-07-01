package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.service.collections.CollectionTreeNodes;
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

public class CollectionTreeClickResolverTest extends AbstractSwingUiTest {

    @Test
    public void rowAtYShouldUseClosestRowWithoutMatchingBlankSpaceBelowRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTree tree = createTree();
            CollectionTreeClickResolver resolver = new CollectionTreeClickResolver(tree);
            Rectangle firstRowBounds = tree.getRowBounds(0);

            assertEquals(resolver.rowAtY(firstRowBounds.y + firstRowBounds.height / 2), 0);
            assertEquals(resolver.rowAtY(firstRowBounds.y + firstRowBounds.height + 20), -1);
        });
    }

    @Test
    public void resolveShouldUseFullWidthRowButRejectBlankSpaceBelowRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTree tree = createTree();
            CollectionTreeClickResolver resolver = new CollectionTreeClickResolver(tree);
            TreePath firstRowPath = tree.getPathForRow(0);
            Rectangle firstRowBounds = tree.getRowBounds(0);

            CollectionTreeClickTarget resolved = resolver.resolve(
                    tree.getWidth() - RequestTreeCellRenderer.MORE_BUTTON_WIDTH - RequestTreeCellRenderer.ADD_BUTTON_WIDTH - 8,
                    firstRowBounds.y + firstRowBounds.height / 2
            );

            assertSame(resolved.path(), firstRowPath);
            assertNull(resolver.resolve(
                    tree.getWidth() - 4,
                    firstRowBounds.y + firstRowBounds.height + 20
            ));
        });
    }

    @Test
    public void resolveShouldDistinguishGroupActionAreas() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTree tree = createTree();
            CollectionTreeClickResolver resolver = new CollectionTreeClickResolver(tree);
            Rectangle rowBounds = tree.getRowBounds(0);
            int rowY = rowBounds.y + rowBounds.height / 2;

            assertEquals(resolver.resolve(tree.getWidth() - 4, rowY).area(), CollectionTreeClickArea.GROUP_MORE_ACTIONS);
            assertEquals(
                    resolver.resolve(tree.getWidth() - RequestTreeCellRenderer.MORE_BUTTON_WIDTH - 4, rowY).area(),
                    CollectionTreeClickArea.GROUP_ADD_REQUEST
            );
            assertEquals(resolver.resolve(rowBounds.x + 80, rowY).area(), CollectionTreeClickArea.ROW);
        });
    }

    @Test
    public void resolveShouldDistinguishExpansionHandleFromGroupRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTree tree = createTree();
            CollectionTreeClickResolver resolver = new CollectionTreeClickResolver(tree);
            Rectangle rowBounds = tree.getRowBounds(0);
            int rowY = rowBounds.y + rowBounds.height / 2;

            assertEquals(resolver.resolve(1, rowY).area(), CollectionTreeClickArea.EXPANSION_HANDLE);
            assertEquals(resolver.resolve(rowBounds.x + 80, rowY).area(), CollectionTreeClickArea.ROW);
        });
    }

    private static JTree createTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CollectionTreePanel.ROOT);
        DefaultMutableTreeNode group = CollectionTreeNodes.groupNode(new RequestGroup("Group"));
        root.add(group);

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(28);
        tree.setSize(300, 120);
        return tree;
    }
}

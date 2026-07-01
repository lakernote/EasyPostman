package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import com.laker.postman.service.collections.CollectionTreeNodes;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Rectangle;
import java.util.Objects;

final class CollectionTreeClickResolver {
    private final JTree tree;

    CollectionTreeClickResolver(JTree tree) {
        this.tree = Objects.requireNonNull(tree, "tree");
    }

    int rowAtY(int y) {
        int row = tree.getClosestRowForLocation(0, y);
        if (row < 0) {
            return -1;
        }
        Rectangle bounds = tree.getRowBounds(row);
        if (bounds == null || y < bounds.y || y >= bounds.y + bounds.height) {
            return -1;
        }
        return row;
    }

    TreePath pathAt(int x, int y) {
        TreePath path = tree.getPathForLocation(x, y);
        if (path != null) {
            return path;
        }

        int row = rowAtY(y);
        return row >= 0 ? tree.getPathForRow(row) : null;
    }

    CollectionTreeClickTarget resolve(int x, int y) {
        TreePath path = pathAt(x, y);
        if (path == null) {
            return null;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return new CollectionTreeClickTarget(path, node, areaFor(node, x, path));
    }

    private CollectionTreeClickArea areaFor(DefaultMutableTreeNode node, int x, TreePath path) {
        if (!CollectionTreeNodes.isGroup(node)) {
            return CollectionTreeClickArea.ROW;
        }
        if (isOnMoreActions(x)) {
            return CollectionTreeClickArea.GROUP_MORE_ACTIONS;
        }
        if (isOnAddRequestAction(x)) {
            return CollectionTreeClickArea.GROUP_ADD_REQUEST;
        }
        if (isExpansionHandle(x, path)) {
            return CollectionTreeClickArea.EXPANSION_HANDLE;
        }
        return CollectionTreeClickArea.ROW;
    }

    private boolean isOnMoreActions(int x) {
        return x >= tree.getWidth() - RequestTreeCellRenderer.MORE_BUTTON_WIDTH;
    }

    private boolean isOnAddRequestAction(int x) {
        int addActionStart = tree.getWidth()
                - RequestTreeCellRenderer.MORE_BUTTON_WIDTH
                - RequestTreeCellRenderer.ADD_BUTTON_WIDTH;
        int addActionEnd = tree.getWidth() - RequestTreeCellRenderer.MORE_BUTTON_WIDTH;
        return x >= addActionStart && x < addActionEnd;
    }

    private boolean isExpansionHandle(int x, TreePath path) {
        int totalIndent = UIManager.getInt("Tree.leftChildIndent") + UIManager.getInt("Tree.rightChildIndent");
        int depth = path.getPathCount() - (tree.isRootVisible() ? 1 : 2);
        int handleEndX = depth * totalIndent + totalIndent;
        return x <= handleEndX;
    }
}

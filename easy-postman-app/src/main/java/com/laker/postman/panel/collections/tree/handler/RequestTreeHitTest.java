package com.laker.postman.panel.collections.tree.handler;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Rectangle;

final class RequestTreeHitTest {
    private RequestTreeHitTest() {
    }

    static int rowAtY(JTree tree, int y) {
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

    static TreePath pathAt(JTree tree, int x, int y) {
        TreePath path = tree.getPathForLocation(x, y);
        if (path != null) {
            return path;
        }

        int row = rowAtY(tree, y);
        return row >= 0 ? tree.getPathForRow(row) : null;
    }
}

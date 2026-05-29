package com.laker.postman.panel.collections.tree;

import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import lombok.experimental.UtilityClass;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

@UtilityClass
class CollectionTreeHoverOverlay {
    void paint(Graphics g, JTree tree) {
        if (!(tree.getCellRenderer() instanceof RequestTreeCellRenderer renderer)) {
            return;
        }
        int row = renderer.getHoveredRow();
        if (row < 0 || row >= tree.getRowCount()) {
            return;
        }
        Rectangle bounds = tree.getRowBounds(row);
        if (bounds == null) {
            return;
        }
        TreePath path = tree.getPathForRow(row);
        if (path == null || tree.isPathSelected(path)) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(CollectionTreeTheme.hoverOverlayColor());
        g2.fillRect(0, bounds.y, tree.getWidth(), bounds.height);
        g2.dispose();
    }
}

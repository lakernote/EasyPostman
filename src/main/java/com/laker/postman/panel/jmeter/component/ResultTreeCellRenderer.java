package com.laker.postman.panel.jmeter.component;

import com.laker.postman.panel.jmeter.model.ResultNodeInfo;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

// 结果树渲染
public class ResultTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
        if (userObj instanceof ResultNodeInfo info) {
            if (info.success) {
                label.setForeground(new Color(0, 128, 0));
            } else {
                label.setForeground(Color.RED);
            }
        }
        return label;
    }
}
package com.laker.postman.panel.jmeter.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.panel.jmeter.model.JMeterTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class JMeterTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode) {
            switch (jtNode.type) {
                case THREAD_GROUP -> label.setIcon(new FlatSVGIcon("icons/user-group.svg", 16, 16));
                case REQUEST -> label.setIcon(new FlatSVGIcon("icons/http.svg", 16, 16));
                case ASSERTION -> label.setIcon(new FlatSVGIcon("icons/warning.svg", 16, 16));
                case TIMER -> label.setIcon(new FlatSVGIcon("icons/time.svg", 16, 16));
                case ROOT -> label.setIcon(new FlatSVGIcon("icons/computer.svg", 16, 16));
            }
        }
        return label;
    }
}
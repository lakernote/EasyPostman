package com.laker.postman.panel.jmeter;

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
                case THREAD_GROUP -> label.setIcon(UIManager.getIcon("FileView.directoryIcon"));
                case REQUEST -> label.setIcon(UIManager.getIcon("FileView.fileIcon"));
                case ASSERTION -> label.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
                case TIMER -> label.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                case ROOT -> label.setIcon(UIManager.getIcon("FileView.computerIcon"));
            }
        }
        return label;
    }
}
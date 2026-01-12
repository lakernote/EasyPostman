package com.laker.postman.panel.performance.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.util.IconUtil;

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
            // 设置图标
            switch (jtNode.type) {
                case THREAD_GROUP -> label.setIcon(IconUtil.createThemed("icons/user-group.svg", 16, 16));
                case REQUEST -> label.setIcon(new FlatSVGIcon("icons/http.svg", 16, 16));
                case ASSERTION -> label.setIcon(IconUtil.createThemed("icons/warning.svg", 16, 16));
                case TIMER -> label.setIcon(IconUtil.createThemed("icons/time.svg", 16, 16));
                case ROOT -> label.setIcon(new FlatSVGIcon("icons/computer.svg", 16, 16));
            }

            // 根据启用状态设置样式
            String text = jtNode.name;
            Font font = label.getFont();

            if (!jtNode.enabled) {
                Color disabledColor = new Color(150, 150, 150);
                if (!sel) {
                    label.setForeground(disabledColor);
                }
                label.setFont(font.deriveFont(Font.ITALIC));
                label.setText("<html><strike>" + text + "</strike></html>");
            } else {
                // 启用状态：恢复正常样式
                label.setFont(font.deriveFont(Font.PLAIN));
                label.setText(text);
            }
        }
        return label;
    }
}
package com.laker.postman.common.component.combobox;

import com.laker.postman.model.Workspace;

import javax.swing.*;
import java.awt.*;

/**
 * 工作区下拉框的渲染器
 */
public class WorkspaceItemRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Workspace workspace) {
            label.setText(workspace.getName());
            // 如果名称太长，使用tooltip显示完整名称
            if (workspace.getName().length() > 15) {
                label.setToolTipText(workspace.getName());
            }
        }
        return label;
    }
}
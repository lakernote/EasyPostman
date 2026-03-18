package com.laker.postman.common.component.combobox;

import com.laker.postman.model.Environment;
import com.laker.postman.model.EnvironmentItem;

import javax.swing.*;
import java.awt.*;

/**
 * 环境下拉框的渲染器
 */
public class EnvironmentItemRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // 使用默认渲染器获取基础组件
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof EnvironmentItem item) {
            Environment env = item.getEnvironment();
            label.setText(env.getName());
        }
        return label;
    }
}
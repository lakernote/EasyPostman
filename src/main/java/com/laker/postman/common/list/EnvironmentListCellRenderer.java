package com.laker.postman.common.list;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.Environment;
import com.laker.postman.model.EnvironmentItem;
import com.laker.postman.service.EnvironmentService;

import javax.swing.*;
import java.awt.*;

import static com.laker.postman.util.JComponentUtils.ellipsisText;

// 环境列表渲染器
public class EnvironmentListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof EnvironmentItem item) {
            String envName = item.getEnvironment().getName(); // 获取环境名称
            label.setText(ellipsisText(envName, list));  // 超出宽度显示省略号
            label.setToolTipText(envName); // 超出显示tip
            Environment active = EnvironmentService.getActiveEnvironment();
            // Postman风格：激活环境加粗+主色icon，普通环境灰icon，选中高亮
            if (active != null && active.getId().equals(item.getEnvironment().getId())) {
                label.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
            } else {
                label.setIcon(new FlatSVGIcon("icons/nocheck.svg", 16, 16));
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2)); // 设置内边距
            label.setIconTextGap(8); // 增大icon和文字间距
            label.setHorizontalAlignment(SwingConstants.LEFT); // 左对齐
            label.setHorizontalTextPosition(SwingConstants.RIGHT); // icon在左，文字在右
        }
        return label;
    }

}
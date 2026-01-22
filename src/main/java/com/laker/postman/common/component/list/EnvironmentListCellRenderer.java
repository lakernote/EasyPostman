package com.laker.postman.common.component.list;

import com.laker.postman.model.Environment;
import com.laker.postman.model.EnvironmentItem;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.util.IconUtil;

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

            // 判断是否是激活环境
            boolean isActive = active != null && active.getId().equals(item.getEnvironment().getId());

            // 设置图标和样式
            Color checkedColor = new Color(0x1488FF); // 浅蓝色，亮/暗主题都适用
            if (isActive) {
                label.setIcon(IconUtil.createColored("icons/check.svg", 18, 18, checkedColor));
                // 激活环境使用加粗字体
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            } else {
                label.setIcon(IconUtil.createThemed("icons/nocheck.svg", 16, 16));
                // 普通环境使用常规字体
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
            }

            // 设置边距和对齐
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setIconTextGap(10); // 增大icon和文字间距
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setHorizontalTextPosition(SwingConstants.RIGHT);

            // 选中状态的样式增强
            if (isSelected) {
                label.setOpaque(true);
            }
        }

        return label;
    }

}
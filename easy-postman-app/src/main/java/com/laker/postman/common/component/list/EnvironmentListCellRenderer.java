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

            // 判断是否是激活环境（需要处理 id 为 null 的情况）
            boolean isActive = false;
            if (active != null && item.getEnvironment() != null) {
                String activeId = active.getId();
                String itemId = item.getEnvironment().getId();
                // 两个 id 都不为 null 时才比较
                if (activeId != null && itemId != null) {
                    isActive = activeId.equals(itemId);
                }
            }

            // 设置图标和样式
            if (isActive) {
                label.setIcon(IconUtil.create("icons/check.svg", 16, 16));
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
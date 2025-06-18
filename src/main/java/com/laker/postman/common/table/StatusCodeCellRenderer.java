package com.laker.postman.common.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * <pre>
 *     状态码单元格渲染器：根据状态码值设置不同颜色
 *      - 2xx：绿色
 *      - 4xx：橙色
 *      - 5xx：红色
 *      - 其他或无效状态码：灰色
 *      使用示例：
 *          table.getColumnModel().getColumn(1).setCellRenderer(new StatusCodeCellRenderer());
 * </pre>
 */
public class StatusCodeCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // 调用父类方法获取默认组件
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // 设置默认字体颜色
        String codeStr = value == null ? "" : value.toString();
        // 根据状态码设置不同颜色
        Color fg = Color.GRAY;
        if (codeStr.matches("\\d+")) {
            int code = Integer.parseInt(codeStr);
            if (code >= 200 && code < 300) fg = new java.awt.Color(0, 153, 0); // 绿色
            else if (code >= 400 && code < 500) fg = new java.awt.Color(255, 140, 0); // 橙色
            else if (code >= 500 && code < 600) fg = java.awt.Color.RED;
        }
        c.setForeground(fg);
        return c;
    }
}
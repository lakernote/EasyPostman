package com.laker.postman.common.component.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * 文本或文件组合单元格渲染器
 * 增强版：支持类型动态配置、自定义文本渲染
 */
public class TextOrFileTableCellRenderer implements TableCellRenderer {

    private final FileCellRenderer fileRenderer = new FileCellRenderer();
    private final DefaultTableCellRenderer textRenderer;

    // 可配置的类型列索引，默认为1
    private int typeColumnIndex = 1;

    // 可选的空值提示文本
    private String emptyValueHint = null;

    /**
     * 创建默认的文本或文件组合渲染器
     */
    public TextOrFileTableCellRenderer() {
        textRenderer = new DefaultTableCellRenderer();
        textRenderer.setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // 从类型列获取单元格类型
        Object type = table.getValueAt(row, typeColumnIndex);
        Component c;

        if (TableUIConstants.FILE_TYPE.equals(type)) {
            c = fileRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            // 对于普通文本单元格，处理空值提示
            if (value == null || value.toString().trim().isEmpty()) {
                if (emptyValueHint != null) {
                    c = textRenderer.getTableCellRendererComponent(table, emptyValueHint, isSelected, hasFocus, row, column);
                    c.setForeground(TableUIConstants.TEXT_DISABLED);
                    c.setFont(c.getFont().deriveFont(Font.ITALIC));
                } else {
                    c = textRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            } else {
                c = textRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }

        int hoverRow = -1;
        Object hoverObj = table.getClientProperty("hoverRow");
        if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;

        String cellValue = value == null ? null : value.toString();
        boolean isEmpty = cellValue == null || cellValue.trim().isEmpty();
        boolean isHovered = row == hoverRow;

        c.setBackground(TableUIConstants.getCellBackground(isSelected, isHovered, isEmpty, row, table));

        // 为文本单元格添加内边距
        if (!(TableUIConstants.FILE_TYPE.equals(type))) {
            if (c instanceof JLabel) {
                ((JLabel) c).setBorder(TableUIConstants.createLabelBorder());
            }
        }

        return c;
    }
}
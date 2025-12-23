package com.laker.postman.common.component.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * 文本或文件组合单元格渲染器
 * 增强版：支持类型动态配置、自定义文本渲染
 */
public class TextOrFileTableCellRenderer implements TableCellRenderer {

    private final FileCellRenderer fileRenderer = new FileCellRenderer();
    private final EasyPostmanTextFieldCellRenderer textRenderer;

    // 可配置的类型列索引，默认为2（Form-Data表格中Type列的索引）
    private final int typeColumnIndex = 2;

    /**
     * 创建默认的文本或文件组合渲染器
     */
    public TextOrFileTableCellRenderer() {
        textRenderer = new EasyPostmanTextFieldCellRenderer();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // 从类型列获取单元格类型
        Object type = table.getValueAt(row, typeColumnIndex);
        Component c;

        if (TableUIConstants.FILE_TYPE.equals(type)) {
            c = fileRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            c = textRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
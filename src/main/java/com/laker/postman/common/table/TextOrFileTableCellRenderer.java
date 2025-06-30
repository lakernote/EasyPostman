package com.laker.postman.common.table;

import com.laker.postman.common.constants.Colors;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class TextOrFileTableCellRenderer implements TableCellRenderer {

    private final FileCellRenderer fileRenderer = new FileCellRenderer();
    private final DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Object type = table.getValueAt(row, 1);
        Component c;
        if ("File".equals(type)) {
            c = fileRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            c = textRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        // 斑马纹、选中、悬停、空值高亮
        int hoverRow = -1;
        Object hoverObj = table.getClientProperty("hoverRow");
        if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;
        String cellValue = value == null ? null : value.toString();
        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
        } else if (row == hoverRow) {
            c.setBackground(new Color(230, 240, 255));
        } else if (cellValue == null || cellValue.trim().isEmpty()) {
            c.setBackground(Colors.EMPTY_CELL_YELLOW);
        } else {
            c.setBackground(row % 2 == 0 ? new Color(250, 252, 255) : Color.WHITE);
        }
        return c;
    }
}
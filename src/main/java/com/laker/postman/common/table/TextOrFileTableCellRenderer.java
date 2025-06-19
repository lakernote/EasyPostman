package com.laker.postman.common.table;

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
        if ("File".equals(type)) {
            return fileRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            return textRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
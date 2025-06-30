package com.laker.postman.common.table;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * 文件类型单元格渲染器
 */
public class FileCellRenderer implements TableCellRenderer {
    public FileCellRenderer() {
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String val = value == null ? "" : value.toString();
        int hoverRow = -1;
        Object hoverObj = table.getClientProperty("hoverRow");
        if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;
        Color zebraColor = row % 2 == 0 ? new Color(250, 252, 255) : Color.WHITE;
        Color selectColor = table.getSelectionBackground();
        Color hoverColor = new Color(230, 240, 255);
        Color emptyColor = com.laker.postman.common.constants.Colors.EMPTY_CELL_YELLOW;
        boolean isEmpty = val.isEmpty() || "选择文件".equals(val);
        if (isEmpty) {
            JButton button = new JButton();
            button.setText("选择文件");
            button.setIcon(IconFontSwing.buildIcon(FontAwesome.FILE, 14, new Color(216, 209, 160)));
            button.setForeground(new Color(216, 209, 160));
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 230)),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (isSelected) {
                button.setBackground(selectColor);
            } else if (row == hoverRow) {
                button.setBackground(hoverColor);
            } else {
                button.setBackground(emptyColor);
            }
            return button;
        } else {
            JLabel label = new JLabel();
            label.setText(val);
            label.setIcon(IconFontSwing.buildIcon(FontAwesome.FILE_O, 14, new Color(76, 130, 206)));
            label.setForeground(new Color(76, 130, 206));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            if (isSelected) {
                label.setBackground(selectColor);
            } else if (row == hoverRow) {
                label.setBackground(hoverColor);
            } else {
                label.setBackground(zebraColor);
            }
            return label;
        }
    }
}
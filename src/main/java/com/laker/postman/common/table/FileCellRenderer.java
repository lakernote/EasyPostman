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
        if (val.isEmpty() || "选择文件".equals(val)) {
            JButton button = new JButton();
            button.setText("选择文件");
            button.setIcon(IconFontSwing.buildIcon(FontAwesome.FILE, 14, new Color(216, 209, 160)));
            button.setForeground(new Color(216, 209, 160));
            return button;
        } else {
            JLabel label = new JLabel();
            label.setText(val);
            label.setIcon(IconFontSwing.buildIcon(FontAwesome.FILE_O, 14, new Color(76, 130, 206)));
            label.setForeground(new Color(76, 130, 206));
            return label;
        }
    }
}
package com.laker.postman.common.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

/**
 * 文件选择单元格编辑器
 * 用于在表格中选择文件路径
 */
public class FileCellEditor extends DefaultCellEditor {
    public static final String COLUMN_TEXT = "选择文件";
    private final JButton button;
    private String filePath;
    private final Component parentComponent;

    public FileCellEditor(Component parentComponent) {
        super(new JCheckBox());
        this.parentComponent = parentComponent;
        button = new JButton(COLUMN_TEXT);
        button.setFocusPainted(false);
        button.setBackground(new Color(250, 252, 255)); // 斑马纹浅色
        button.setForeground(new Color(66, 133, 244)); // 主题蓝色
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(COLUMN_TEXT);
            int result = fileChooser.showOpenDialog(parentComponent);
            if (result == JFileChooser.APPROVE_OPTION) {
                filePath = fileChooser.getSelectedFile().getAbsolutePath();
                button.setText(filePath);
                button.setForeground(new Color(76, 130, 206));
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        filePath = value == null ? "" : value.toString();
        if (filePath.isEmpty() || COLUMN_TEXT.equals(filePath)) {
            button.setText(COLUMN_TEXT);
            button.setIcon(new FlatSVGIcon("icons/file.svg", 18, 18));
            button.setForeground(new Color(66, 133, 244));
            button.setBackground(row % 2 == 0 ? new Color(250, 252, 255) : Color.WHITE);
        } else {
            button.setText(filePath);
            button.setIcon(new FlatSVGIcon("icons/file.svg", 18, 18));
            button.setForeground(new Color(76, 130, 206));
            button.setBackground(row % 2 == 0 ? new Color(250, 252, 255) : Color.WHITE);
        }
        if (isSelected) {
            button.setBackground(table.getSelectionBackground());
        }
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return filePath == null ? "" : filePath;
    }
}
package com.laker.postman.common.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * 错误信息单元格渲染器：用于在表格中显示错误信息，支持自动换行和悬浮提示
 * <p>
 * 使用示例：
 * <pre>
 *     table.getColumnModel().getColumn(4).setCellRenderer(new ErrorMessageCellRenderer());
 * </pre>
 */
public class ErrorMessageCellRenderer extends JTextArea implements TableCellRenderer {
    public ErrorMessageCellRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setFont(new JLabel().getFont());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        String text = value == null ? "" : value.toString();
        setText(text);
        setToolTipText(text); // 悬浮显示完整内容

        // 设置背景色
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        // 自动调整高度
        int preferredHeight = getPreferredSize().height;
        if (table.getRowHeight(row) != preferredHeight) {
            table.setRowHeight(row, Math.min(preferredHeight, 60)); // 限制最大高度
        }
        return this;
    }
}
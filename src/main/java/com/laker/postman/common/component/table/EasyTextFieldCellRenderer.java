package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class EasyTextFieldCellRenderer extends EasyTextField implements TableCellRenderer {
    public EasyTextFieldCellRenderer() {
        super(1);
        setBorder(null);
        setOpaque(true);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 主题切换后重新设置边框为null，避免显示边框
        setBorder(null);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value == null ? "" : value.toString());

        if (value == null || value.toString().isEmpty()) {
            // 空单元格：使用主题适配的空单元格背景色（有区分度）
            setBackground(ModernColors.getEmptyCellBackground());
        } else if (isSelected) {
            // 选中状态：使用选中背景色
            setBackground(table.getSelectionBackground());
        } else {
            // 有值且非选中：使用表格背景色
            setBackground(table.getBackground());
        }

        return this;
    }
}
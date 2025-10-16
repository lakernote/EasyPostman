package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyPostmanTextField;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class EasyPostmanTextFieldCellRenderer extends EasyPostmanTextField implements TableCellRenderer {
    public EasyPostmanTextFieldCellRenderer() {
        super(1);
        setBorder(null);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value == null ? "" : value.toString());
        if (value == null || value.toString().isEmpty()) {
            setBackground(Color.WHITE);
        } else if (isSelected) {
            setBackground(table.getSelectionBackground());
        } else {
            setBackground(table.getBackground());
        }
        return this;
    }
}
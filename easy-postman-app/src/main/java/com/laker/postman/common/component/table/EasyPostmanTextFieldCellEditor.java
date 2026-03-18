package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class EasyPostmanTextFieldCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final EasyTextField textField = new EasyTextField(1);

    public EasyPostmanTextFieldCellEditor() {
        textField.setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return textField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        textField.setText(value == null ? "" : value.toString());
        return textField;
    }
}
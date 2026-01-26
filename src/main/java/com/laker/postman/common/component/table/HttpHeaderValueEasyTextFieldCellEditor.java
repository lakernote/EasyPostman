package com.laker.postman.common.component.table;

import com.laker.postman.common.component.AutoCompleteEasyTextField;
import com.laker.postman.util.HttpHeaderConstants;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/**
 * HTTP Header Value 智能编辑器
 * 基于 AutoCompleteEasyTextField，保留变量功能
 * 根据对应的 Header Key 提供智能补全
 */
public class HttpHeaderValueEasyTextFieldCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final AutoCompleteEasyTextField textField;
    private final int keyColumnIndex;
    private JTable parentTable;
    private int editingRow;

    public HttpHeaderValueEasyTextFieldCellEditor(int keyColumnIndex) {
        this.keyColumnIndex = keyColumnIndex;
        this.textField = new AutoCompleteEasyTextField(1);
        this.textField.setBorder(null);

        // 获得焦点时根据 Key 显示建议
        this.textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(HttpHeaderValueEasyTextFieldCellEditor.this::updateSuggestionsForCurrentKey);
            }
        });
    }

    private void updateSuggestionsForCurrentKey() {
        if (parentTable == null || editingRow < 0) {
            textField.setAutoCompleteEnabled(false);
            return;
        }

        // 获取对应的 Key 值
        Object keyValue = parentTable.getModel().getValueAt(editingRow, keyColumnIndex);
        if (keyValue == null) {
            textField.setAutoCompleteEnabled(false);
            return;
        }

        String headerKey = keyValue.toString().trim();
        List<String> suggestions = HttpHeaderConstants.getCommonValuesForHeader(headerKey);

        if (suggestions.isEmpty()) {
            textField.setAutoCompleteEnabled(false);
        } else {
            textField.setSuggestions(suggestions);
            textField.setAutoCompleteEnabled(true);
            if (textField.getText().isEmpty()) {
                textField.showAllSuggestions();
            }
        }
    }

    @Override
    public Object getCellEditorValue() {
        return textField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        this.parentTable = table;
        this.editingRow = row;

        textField.setText(value == null ? "" : value.toString());
        return textField;
    }
}

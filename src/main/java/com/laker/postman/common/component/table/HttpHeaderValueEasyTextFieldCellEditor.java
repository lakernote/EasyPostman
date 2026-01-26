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
    private String currentHeaderKey;

    public HttpHeaderValueEasyTextFieldCellEditor(int keyColumnIndex) {
        this.keyColumnIndex = keyColumnIndex;
        this.textField = new AutoCompleteEasyTextField(1);
        this.textField.setBorder(null);

        // 优化：获得焦点时根据 Key 更新建议
        this.textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateSuggestionsForCurrentKey();
            }
        });
    }

    private void updateSuggestionsForCurrentKey() {
        if (parentTable == null || editingRow < 0 || editingRow >= parentTable.getModel().getRowCount()) {
            disableAutoComplete();
            return;
        }

        // 获取对应的 Key 值
        Object keyValue = parentTable.getModel().getValueAt(editingRow, keyColumnIndex);
        String headerKey = (keyValue != null) ? keyValue.toString().trim() : "";

        // 优化：只在 headerKey 改变时才更新 suggestions
        if (headerKey.isEmpty() || headerKey.equals(currentHeaderKey)) {
            if (headerKey.isEmpty()) {
                disableAutoComplete();
            } else {
                // headerKey 没变，只需要显示建议（如果已启用）
                showSuggestionsIfEmpty();
            }
            return;
        }

        currentHeaderKey = headerKey;
        List<String> suggestions = HttpHeaderConstants.getCommonValuesForHeader(headerKey);

        if (suggestions == null || suggestions.isEmpty()) {
            disableAutoComplete();
        } else {
            textField.setSuggestions(suggestions);
            textField.setAutoCompleteEnabled(true);
            showSuggestionsIfEmpty();
        }
    }

    private void disableAutoComplete() {
        textField.setAutoCompleteEnabled(false);
        currentHeaderKey = null;
    }

    private void showSuggestionsIfEmpty() {
        // 只有当值为空时才自动显示所有建议
        String currentText = textField.getText();
        if (currentText == null || currentText.trim().isEmpty()) {
            SwingUtilities.invokeLater(textField::showAllSuggestions);
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

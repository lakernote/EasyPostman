package com.laker.postman.common.component.table;

import com.laker.postman.common.component.AutoCompleteEasyTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/**
 * 支持自动补全的单元格编辑器
 * 保留 EasyTextField 的所有功能（变量高亮、撤销重做等）
 * 新增自动补全功能
 */
public class HttpHeaderKeyEasyTextFieldCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final AutoCompleteEasyTextField textField;

    public HttpHeaderKeyEasyTextFieldCellEditor(List<String> suggestions) {
        this.textField = new AutoCompleteEasyTextField(1);
        this.textField.setBorder(null);
        this.textField.setSuggestions(suggestions);
        this.textField.setAutoCompleteEnabled(true);

        // 获得焦点时显示建议
        this.textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (textField.getText().isEmpty()) {
                        textField.showAllSuggestions();
                    }
                });
            }
        });
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

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

        // 获得焦点时只在文本为空时显示建议
        // 如果已经有内容，让用户主动编辑时再触发自动补全
        this.textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String text = textField.getText();
                    // 只有当文本为空时才自动显示所有建议
                    if (text == null || text.trim().isEmpty()) {
                        textField.showAllSuggestions();
                    }
                    // 如果文本不为空，用户需要主动编辑才会触发自动补全
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

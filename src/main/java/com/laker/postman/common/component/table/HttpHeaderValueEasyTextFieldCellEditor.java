package com.laker.postman.common.component.table;

import com.laker.postman.common.component.AutoCompleteEasyTextField;
import com.laker.postman.util.HttpHeaderConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;
import java.util.List;

/**
 * HTTP Header Value 智能编辑器
 * 继承 EasySmartValueCellEditor，复用智能多行编辑能力
 * 同时支持根据对应的 Header Key 提供智能补全
 */
public class HttpHeaderValueEasyTextFieldCellEditor extends EasySmartValueCellEditor {
    private final int keyColumnIndex;
    private final AutoCompleteEasyTextField autoCompleteTextField;
    private JTable parentTable;
    private int editingRow;

    public HttpHeaderValueEasyTextFieldCellEditor(int keyColumnIndex) {
        super(true); // 启用自动多行编辑
        this.keyColumnIndex = keyColumnIndex;

        // 将父类的单行 textField 替换为 AutoCompleteEasyTextField
        this.autoCompleteTextField = new AutoCompleteEasyTextField(1);
        this.autoCompleteTextField.setBorder(null);

        // 优化：获得焦点时，如果文本为空则显示所有建议
        this.autoCompleteTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // 只在文本为空时自动显示所有建议
                String text = autoCompleteTextField.getText();
                if (text == null || text.trim().isEmpty()) {
                    SwingUtilities.invokeLater(autoCompleteTextField::showAllSuggestions);
                }
            }
        });

        // 通过父类方法替换单行组件，同步更新 CardLayout 卡片和 DocumentListener
        replaceTextField(autoCompleteTextField);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        this.parentTable = table;
        this.editingRow = row;

        // 调用父类方法（父类始终返回 containerPanel）
        Component editor = super.getTableCellEditorComponent(table, value, isSelected, row, column);

        // 只要当前是单行模式，就更新自动补全建议
        if (!isMultiLineMode()) {
            updateSuggestionsForCurrentKey();
        }

        return editor;
    }

    private void updateSuggestionsForCurrentKey() {
        if (parentTable == null || editingRow < 0 || editingRow >= parentTable.getModel().getRowCount()) {
            disableAutoComplete();
            return;
        }

        // 获取对应的 Key 值
        Object keyValue = parentTable.getModel().getValueAt(editingRow, keyColumnIndex);
        String headerKey = (keyValue != null) ? keyValue.toString().trim() : "";

        // 如果 Key 为空，禁用自动补全
        if (headerKey.isEmpty()) {
            disableAutoComplete();
            return;
        }

        // 获取对应的推荐值列表
        List<String> suggestions = HttpHeaderConstants.getCommonValuesForHeader(headerKey);

        if (suggestions == null || suggestions.isEmpty()) {
            disableAutoComplete();
        } else {
            autoCompleteTextField.setSuggestions(suggestions);
            autoCompleteTextField.setAutoCompleteEnabled(true);

            // 如果文本为空，自动显示所有建议
            String currentText = autoCompleteTextField.getText();
            if (currentText == null || currentText.trim().isEmpty()) {
                SwingUtilities.invokeLater(autoCompleteTextField::showAllSuggestions);
            }
        }
    }

    private void disableAutoComplete() {
        if (autoCompleteTextField != null) {
            autoCompleteTextField.setAutoCompleteEnabled(false);
            // 清除推荐列表，避免下次启用时显示旧的推荐
            autoCompleteTextField.setSuggestions(Collections.emptyList());
        }
    }
}

package com.laker.postman.common.table;

import cn.hutool.core.text.CharSequenceUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 带验证功能的表格单元格编辑器
 * 支持非空验证，验证失败时显示红色边框并阻止编辑完成
 */
public class ValidatingTableCellEditor extends DefaultCellEditor {
    private final JTextField textField;
    private boolean allowEmpty;
    private String validationMessage;

    public ValidatingTableCellEditor() {
        this(false, "此字段不能为空");
    }

    public ValidatingTableCellEditor(boolean allowEmpty, String validationMessage) {
        super(new JTextField());
        this.textField = (JTextField) getComponent();
        this.allowEmpty = allowEmpty;
        this.validationMessage = validationMessage;

        // 设置默认样式
        textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
    }

    @Override
    public boolean stopCellEditing() {
        String value = textField.getText();

        // 验证非空
        if (!allowEmpty && CharSequenceUtil.isBlank(value)) {
            // 设置红色边框表示验证失败
            textField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

            // 显示提示消息
            JOptionPane.showMessageDialog(textField, validationMessage, "验证失败", JOptionPane.WARNING_MESSAGE);

            // 让文本框获得焦点，便于用户修改
            SwingUtilities.invokeLater(() -> {
                textField.requestFocus();
                textField.selectAll();
            });

            return false; // 阻止编辑完成
        }

        // 验证通过，恢复正常边框
        textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        return super.stopCellEditing();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Component comp = super.getTableCellEditorComponent(table, value, isSelected, row, column);
        // 重置边框样式
        textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        return comp;
    }
}

package com.laker.postman.common.table;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * 带验证功能的表格单元格编辑器
 * 支持非空验证，验证失败时显示红色边框并阻止编辑完成
 * 增强功能：支持实时验证和跨列验证
 */
@Slf4j
public class ValidatingTableCellEditor extends DefaultCellEditor {
    private final JTextField textField;
    private boolean allowEmpty;
    private String validationMessage;
    private JTable currentTable;
    private int currentRow;
    private int currentColumn;
    private boolean enableCrossColumnValidation = false;
    private int nameColumnIndex = 0; // Name列索引
    private int valueColumnIndex = 1; // Value列索引

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

        // 添加实时验证
        addRealTimeValidation();
    }

    /**
     * 启用跨列验证（用于Name-Value表格）
     *
     * @param nameColumnIndex  Name列的索引
     * @param valueColumnIndex Value列的索引
     */
    public void enableCrossColumnValidation(int nameColumnIndex, int valueColumnIndex) {
        this.enableCrossColumnValidation = true;
        this.nameColumnIndex = nameColumnIndex;
        this.valueColumnIndex = valueColumnIndex;
    }

    /**
     * 添加实时验证功能
     */
    private void addRealTimeValidation() {
        // 文本变化时实时验证
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateInRealTime();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateInRealTime();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateInRealTime();
            }
        });

        // 失去焦点时验证
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateOnFocusLost();
            }
        });
    }

    /**
     * 实时验证（输入时）
     */
    private void validateInRealTime() {
        if (currentTable == null) return;

        SwingUtilities.invokeLater(() -> {
            String currentValue = textField.getText();
            boolean isValid = true;

            // 基础非空验证
            if (!allowEmpty && CharSequenceUtil.isBlank(currentValue)) {
                isValid = false;
            }

            // 跨列验证：如果当前是Value列，检查Name列是否为空
            if (enableCrossColumnValidation && isValid) {
                isValid = validateCrossColumn(currentValue);
            }

            // 设置边框颜色
            if (isValid) {
                textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            } else {
                textField.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 1)); // 橙色表示警告
            }
        });
    }

    /**
     * 失去焦点时验证
     */
    private void validateOnFocusLost() {
        if (currentTable == null) return;

        String currentValue = textField.getText();

        // 跨列验证：如果在Value列输入了内容但Name列为空，显示提示
        if (enableCrossColumnValidation && currentColumn == valueColumnIndex) {
            Object nameValue = currentTable.getValueAt(currentRow, nameColumnIndex);
            String nameStr = nameValue == null ? "" : nameValue.toString().trim();

            if (!CharSequenceUtil.isBlank(currentValue) && CharSequenceUtil.isBlank(nameStr)) {
                // 显示友好提示
                showWarningTooltip(I18nUtil.getMessage("env.validation.name.required"));
                textField.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
            }
        }
    }

    /**
     * 跨列验证
     */
    private boolean validateCrossColumn(String currentValue) {
        if (currentColumn == valueColumnIndex && !CharSequenceUtil.isBlank(currentValue)) {
            // 在Value列输入内容时，检查Name列
            Object nameValue = currentTable.getValueAt(currentRow, nameColumnIndex);
            String nameStr = nameValue == null ? "" : nameValue.toString().trim();
            return !CharSequenceUtil.isBlank(nameStr);
        }
        return true;
    }

    /**
     * 显示警告提示
     */
    private void showWarningTooltip(String message) {
        // 检查组件是否可见和已显示在屏幕上
        if (!textField.isShowing() || !textField.isDisplayable()) {
            return; // 如果组件不可见，直接返回
        }

        try {
            // 创建一个临时的提示标签
            JLabel tipLabel = new JLabel(message);
            tipLabel.setOpaque(true);
            tipLabel.setBackground(new Color(255, 252, 220)); // 浅黄色背景
            tipLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.ORANGE, 1),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)
            ));
            tipLabel.setFont(tipLabel.getFont().deriveFont(11f));

            // 显示提示并在3秒后消失
            Point location = textField.getLocationOnScreen();
            JWindow tipWindow = new JWindow();
            tipWindow.add(tipLabel);
            tipWindow.pack();
            tipWindow.setLocation(location.x, location.y + textField.getHeight());
            tipWindow.setVisible(true);

            // 3秒后自动隐藏
            Timer timer = new Timer(3000, e -> {
                if (tipWindow.isDisplayable()) {
                    tipWindow.dispose();
                }
            });
            timer.setRepeats(false);
            timer.start();
        } catch (IllegalComponentStateException ex) {
            // 如果获取位置失败，就不显示提示窗口
            // 这通常发生在组件正在被移除或隐藏时
            log.debug("无法显示提示窗口，组件不可见: {}", ex.getMessage());
        }
    }

    @Override
    public boolean stopCellEditing() {
        String value = textField.getText();

        // 基础非空验证（仅对不允许为空的列）
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

        // 跨列验证：如果在Value列输入了内容但Name列为空
        if (enableCrossColumnValidation && currentColumn == valueColumnIndex && !CharSequenceUtil.isBlank(value)) {
            Object nameValue = currentTable.getValueAt(currentRow, nameColumnIndex);
            String nameStr = nameValue == null ? "" : nameValue.toString().trim();

            if (CharSequenceUtil.isBlank(nameStr)) {
                // 不阻止编辑完成，只是提示用户
                textField.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));

                // 使用温和的提示方式，不阻止操作
                SwingUtilities.invokeLater(() -> {
                    showWarningTooltip("建议先输入变量名");
                    // 可选：自动将焦点转移到Name列
                    if (currentTable.editCellAt(currentRow, nameColumnIndex)) {
                        currentTable.getEditorComponent().requestFocus();
                    }
                });

                // 允许编辑完成，不阻止用户操作
            }
        }

        // 验证通过，恢复正常边框
        textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        return super.stopCellEditing();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Component comp = super.getTableCellEditorComponent(table, value, isSelected, row, column);

        // 记录当前编辑的表格和位置
        this.currentTable = table;
        this.currentRow = row;
        this.currentColumn = column;

        // 重置边框样式
        textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        return comp;
    }
}

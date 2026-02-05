package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.button.EditButton;
import com.laker.postman.common.component.table.EasyVariableTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Variable;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Variables 面板包装器，提供批量编辑功能
 */
public class EasyVariablesPanel extends JPanel {
    private EasyVariableTablePanel tablePanel;

    public EasyVariablesPanel() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // Create header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        JLabel label = new JLabel("Variables");

        // Create Bulk Edit button
        EditButton bulkEditButton = new EditButton();
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BULK_EDIT));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());

        headerPanel.add(label);
        headerPanel.add(Box.createHorizontalStrut(5)); // 添加间距
        headerPanel.add(bulkEditButton);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void setupLayout() {
        // Create table panel
        tablePanel = new EasyVariableTablePanel();
        add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * 显示批量编辑对话框
     * 支持以 "Key: Value" 格式批量粘贴和编辑变量
     */
    private void showBulkEditDialog() {
        // 1. 将当前表格数据转换为文本格式（Key: Value\n）
        StringBuilder text = new StringBuilder();
        List<Variable> currentVariables = tablePanel.getVariableList();
        for (Variable variable : currentVariables) {
            if (!variable.getKey().isEmpty()) {
                text.append(variable.getKey()).append(": ").append(variable.getValue()).append("\n");
            }
        }

        // 2. 创建文本编辑区域
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        // 设置背景色，使其看起来像可编辑区域
        textArea.setBackground(ModernColors.getInputBackgroundColor());
        textArea.setForeground(ModernColors.getTextPrimary());
        textArea.setCaretColor(ModernColors.PRIMARY);

        // 将光标定位到文本末尾
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // 3. 创建提示标签 - 使用国际化，垂直排列
        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        // 主提示文本
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        // 支持格式说明
        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6)); // 添加间距
        hintPanel.add(formatLabel);
        hintPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 4. 组装内容面板
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.add(hintPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 5. 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // 6. 创建自定义对话框
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, I18nUtil.getMessage(MessageKeys.BULK_EDIT_VARIABLES), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 设置对话框属性
        dialog.setSize(650, 400);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        // 7. 按钮事件处理
        okButton.addActionListener(e -> {
            parseBulkText(textArea.getText());
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // 8. 支持 ESC 键关闭对话框
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 9. 设置默认按钮
        dialog.getRootPane().setDefaultButton(okButton);

        // 10. 显示对话框
        dialog.setVisible(true);
    }

    /**
     * 解析批量编辑的文本内容
     * 支持格式：
     * - Key: Value
     * - Key:Value
     * - Key = Value
     * - Key=Value
     * 空行和注释会被忽略
     */
    private void parseBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            // 如果文本为空，清空所有变量
            tablePanel.setVariableList(new ArrayList<>());
            return;
        }

        List<Variable> variables = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue; // 忽略空行和注释
            }

            // 支持 ":" 和 "=" 两种分隔符
            String[] parts = null;
            if (line.contains(":")) {
                parts = line.split(":", 2);
            } else if (line.contains("=")) {
                parts = line.split("=", 2);
            }

            if (parts != null && parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, value));
                }
            } else if (line.contains(":") || line.contains("=")) {
                // 如果包含分隔符但解析失败，可能是值为空的情况
                String key = line.replaceAll("[=:].*", "").trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, ""));
                }
            }
        }

        // 更新表格数据
        tablePanel.setVariableList(variables);
    }

    // ========== 代理方法，暴露给外部使用 ==========

    public void addTableModelListener(TableModelListener listener) {
        tablePanel.addTableModelListener(listener);
    }

    public List<Variable> getVariableList() {
        return tablePanel.getVariableList();
    }

    public void setVariableList(List<Variable> variableList) {
        tablePanel.setVariableList(variableList);
    }

    public void stopCellEditing() {
        tablePanel.stopCellEditing();
    }

    public void clear() {
        tablePanel.clear();
    }
}

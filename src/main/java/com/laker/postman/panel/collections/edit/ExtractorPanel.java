package com.laker.postman.panel.collections.edit;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.table.map.EasyTablePanel;
import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpRequestItem;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.Supplier;

/**
 * 变量提取相关的UI和逻辑，独立为ExtractorPanel
 */
public class ExtractorPanel extends JPanel {
    private EasyTablePanel extractorTablePanel;
    public JCheckBox autoExtractCheckBox;

    // 变量提取逻辑
    @Setter
    private String rawResponseBodyText; // 由外部设置
    @Setter
    private Supplier<java.util.List<HttpRequestItem.ExtractorRule>> rulesSupplier; // 规则获取器
    @Setter
    private Supplier<Environment> envSupplier; // 环境获取器
    @Setter
    private Runnable refreshEnvPanel; // 刷新环境面板

    public ExtractorPanel() {
        setLayout(new BorderLayout());
        // 用EasyTablePanel替换表格
        extractorTablePanel = new EasyTablePanel(new String[]{"Name", "JsonPath", "Action"});
        // 设置自适应最后一列，消除右侧空白
        extractorTablePanel.setAutoResizeLastColumn();
        // 设置“操作”列为按钮
        extractorTablePanel.setColumnRenderer(2, new ButtonRenderer());
        extractorTablePanel.setColumnEditor(2, new ExtractButtonEditor());
        // 设置前两列宽度，最后一列自适应
        extractorTablePanel.setColumnPreferredWidth(0, 200);
        extractorTablePanel.setColumnPreferredWidth(1, 500);
        extractorTablePanel.setColumnMinWidth(2, 50);
        extractorTablePanel.setColumnMaxWidth(2, 200);
        int totalWidth = 200 + 500 + 50;
        extractorTablePanel.setPreferredScrollableViewportHeight(8, totalWidth);
        JScrollPane scrollPane = new JScrollPane(extractorTablePanel);
        add(scrollPane, BorderLayout.CENTER);
        // 底部按钮面板
        JPanel extractorBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton helpBtn = new JButton("Help");
        helpBtn.addActionListener(e -> showExtractorHelp());
        autoExtractCheckBox = new JCheckBox("Auto Extract", true);
        extractorBtnPanel.add(helpBtn);
        extractorBtnPanel.add(Box.createHorizontalStrut(20));
        extractorBtnPanel.add(autoExtractCheckBox);
        add(extractorBtnPanel, BorderLayout.SOUTH);
    }

    // 按钮渲染器
    static class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("");
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setIcon(new FlatSVGIcon("icons/extract.svg", 20, 20));
            setHorizontalAlignment(CENTER);
            return this;
        }
    }

    // 按钮编辑器
    class ExtractButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;

        public ExtractButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            button.setIcon(new FlatSVGIcon("icons/extract.svg", 20, 20));
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int row = extractorTablePanel.getEditingRow();
                if (row >= 0 && row < extractorTablePanel.getRowCount()) {
                    String varName = (String) extractorTablePanel.getValueAt(row, 0);
                    String jsonPath = (String) extractorTablePanel.getValueAt(row, 1);
                    extractVariable(varName, jsonPath);
                }
            }
            isPushed = false;
            return new FlatSVGIcon("icons/extract.svg", 20, 20);
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // 显示帮助
    private void showExtractorHelp() {
        String helpText =
                "<html><body style='width: 300px'>" +
                        "<h3>变量提取使用说明</h3>" +
                        "<p>本功能可以从JSON响应中提取数据并保存到环境变量中</p>" +
                        "<h4>JSON路径示例:</h4>" +
                        "<ul>" +
                        "<li><b>$.name</b> - 提取根对象的name属性</li>" +
                        "<li><b>$.data.id</b> - 提取data对象的id属性</li>" +
                        "<li><b>$.items[0].id</b> - 提取items数组第一项的id属性</li>" +
                        "<li><b>$.headers.Content-Type</b> - 提取headers对象的Content-Type属性</li>" +
                        "</ul>" +
                        "<p>提取的变量会保存到当前激活的环境变量中</p>" +
                        "</body></html>";
        JOptionPane.showMessageDialog(null, new JLabel(helpText),
                "变量提取帮助", JOptionPane.INFORMATION_MESSAGE);
    }

    // 提取变量
    private void extractVariable(String varName, String jsonPath) {
        if (varName == null || varName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入变量名", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入JSON路径", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String responseText = rawResponseBodyText;
        if (responseText == null || responseText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "响应体为空，无法提取", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (!cn.hutool.json.JSONUtil.isTypeJSON(responseText)) {
                JOptionPane.showMessageDialog(this, "响应不是有效的JSON格式", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Object json = cn.hutool.json.JSONUtil.parse(responseText);
            String value = com.laker.postman.util.JsonPathUtil.extractJsonPath(json, jsonPath);
            if (value != null) {
                Environment activeEnv = envSupplier != null ? envSupplier.get() : null;
                if (activeEnv == null) {
                    JOptionPane.showMessageDialog(this, "没有激活的环境，请先在环境面板激活一��环境", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                activeEnv.addVariable(varName, value);
                com.laker.postman.service.EnvironmentService.saveEnvironment(activeEnv);
                if (refreshEnvPanel != null) refreshEnvPanel.run();
                JOptionPane.showMessageDialog(this, "成功提取变量", "提取成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "无法找到指定路径的值: " + jsonPath, "提取失败", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "提取变量失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 自动执行所有变量提取规则
    public void autoExecuteExtractorRules() {
        if (!autoExtractCheckBox.isSelected() || rawResponseBodyText == null || rawResponseBodyText.trim().isEmpty()) {
            return;
        }
        try {
            if (!cn.hutool.json.JSONUtil.isTypeJSON(rawResponseBodyText)) {
                return;
            }
            Object json = cn.hutool.json.JSONUtil.parse(rawResponseBodyText);
            Environment activeEnv = envSupplier != null ? envSupplier.get() : null;
            if (activeEnv == null) return;
            java.util.List<HttpRequestItem.ExtractorRule> rules = rulesSupplier != null ? rulesSupplier.get() : null;
            if (rules != null && !rules.isEmpty()) {
                boolean hasExtracted = false;
                for (HttpRequestItem.ExtractorRule rule : rules) {
                    String varName = rule.getVariableName();
                    String jsonPath = rule.getJsonPath();
                    if (varName != null && !varName.trim().isEmpty() && jsonPath != null && !jsonPath.trim().isEmpty()) {
                        String value = com.laker.postman.util.JsonPathUtil.extractJsonPath(json, jsonPath);
                        if (value != null) {
                            hasExtracted = true;
                            activeEnv.addVariable(varName, value);
                        }
                    }
                }
                if (hasExtracted) {
                    com.laker.postman.service.EnvironmentService.saveEnvironment(activeEnv);
                    if (refreshEnvPanel != null) refreshEnvPanel.run();
                }
            }
        } catch (Exception ignored) {
        }
    }

    // 从请求对象加载提取规则到表格中
    public void loadExtractorRules(java.util.List<HttpRequestItem.ExtractorRule> rules, boolean autoExtract) {
        extractorTablePanel.clear();
        if (rules != null) {
            for (HttpRequestItem.ExtractorRule rule : rules) {
                extractorTablePanel.addRow(rule.getVariableName(), rule.getJsonPath(), "Extract");
            }
        }
        autoExtractCheckBox.setSelected(autoExtract);
    }

    // 获取当前表格中的提取规则
    public java.util.List<HttpRequestItem.ExtractorRule> getExtractorRules() {
        java.util.List<HttpRequestItem.ExtractorRule> rules = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> rows = extractorTablePanel.getRows();
        for (java.util.Map<String, Object> row : rows) {
            String varName = row.get("Name") == null ? null : row.get("Name").toString();
            String jsonPath = row.get("JsonPath") == null ? null : row.get("JsonPath").toString();
            if (varName != null && !varName.trim().isEmpty() && jsonPath != null && !jsonPath.trim().isEmpty()) {
                rules.add(new HttpRequestItem.ExtractorRule(varName, jsonPath));
            }
        }
        return rules;
    }

    public boolean isAutoExtract() {
        return autoExtractCheckBox.isSelected();
    }

    public EasyTablePanel getExtractorTablePabel() {
        return extractorTablePanel;
    }
}
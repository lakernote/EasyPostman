package com.laker.postman.panel.jmeter;

import javax.swing.*;
import java.awt.*;

public class AssertionPropertyPanel extends JPanel {
    private final JComboBox<String> typeCombo;
    private final JComboBox<String> operatorCombo; // 仅Response Code用
    private final JTextField responseCodeValueField; // 仅Response Code用
    private final JTextField containsContentField; // Contains用
    private final JTextField jsonPathField; // JSONPath用
    private final JTextField jsonPathExpectField; // JSONPath用
    private JMeterTreeNode currentNode;
    private final CardLayout inputCardLayout;
    private final JPanel inputPanel;
    private final JPanel responseCodePanel;
    private final JPanel containsPanel;
    private final JPanel jsonPathPanel;

    AssertionPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(420, 120));
        setPreferredSize(new Dimension(380, 100));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(new JLabel("断言类型:"), gbc);
        gbc.gridx = 1;
        typeCombo = new JComboBox<>(new String[]{
                "Response Code",
                "Contains",
                "JSONPath"
        });
        add(typeCombo, gbc);

        // 输入区采用CardLayout
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        inputCardLayout = new CardLayout();
        inputPanel = new JPanel(inputCardLayout);

        // Response Code面板
        responseCodePanel = new JPanel(new GridBagLayout());
        GridBagConstraints rcGbc = new GridBagConstraints();
        rcGbc.insets = new Insets(2, 2, 2, 2);
        rcGbc.gridx = 0;
        rcGbc.gridy = 0;
        rcGbc.weightx = 0;
        rcGbc.fill = GridBagConstraints.HORIZONTAL;
        responseCodePanel.add(new JLabel("符号:"), rcGbc);
        rcGbc.gridx = 1;
        operatorCombo = new JComboBox<>(new String[]{"=", ">", "<"});
        responseCodePanel.add(operatorCombo, rcGbc);
        rcGbc.gridx = 2;
        responseCodePanel.add(new JLabel("值:"), rcGbc);
        rcGbc.gridx = 3;
        responseCodeValueField = new JTextField(6);
        responseCodePanel.add(responseCodeValueField, rcGbc);

        // Contains面板
        containsPanel = new JPanel(new BorderLayout(2, 2));
        containsPanel.add(new JLabel("包含内容:"), BorderLayout.WEST);
        containsContentField = new JTextField();
        containsPanel.add(containsContentField, BorderLayout.CENTER);

        // JSONPath面板
        jsonPathPanel = new JPanel(new GridBagLayout());
        GridBagConstraints jpGbc = new GridBagConstraints();
        jpGbc.insets = new Insets(2, 2, 2, 2);
        jpGbc.gridx = 0;
        jpGbc.gridy = 0;
        jpGbc.weightx = 0;
        jpGbc.fill = GridBagConstraints.HORIZONTAL;
        jsonPathPanel.add(new JLabel("JsonPath:"), jpGbc);
        jpGbc.gridx = 1;
        jpGbc.weightx = 1.0; // 让文本框填满剩余空间
        jpGbc.fill = GridBagConstraints.HORIZONTAL; // 水平填充
        jsonPathField = new JTextField(10);
        jsonPathPanel.add(jsonPathField, jpGbc);
        jpGbc.gridx = 0;
        jpGbc.gridy = 1;
        jpGbc.weightx = 0;
        jpGbc.fill = GridBagConstraints.HORIZONTAL;
        jsonPathPanel.add(new JLabel("对比值:"), jpGbc);
        jpGbc.gridx = 1;
        jpGbc.weightx = 1.0;
        jpGbc.fill = GridBagConstraints.HORIZONTAL;
        jsonPathExpectField = new JTextField();
        jsonPathPanel.add(jsonPathExpectField, jpGbc);

        inputPanel.add(responseCodePanel, "Response Code");
        inputPanel.add(containsPanel, "Contains");
        inputPanel.add(jsonPathPanel, "JSONPath");
        add(inputPanel, gbc);

        // 帮助说明
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JLabel helpLabel = new JLabel("<html>\n<ul style='margin-left:10px'>\n<li><b>Response Code</b>: 断言响应码，支持 <b>=</b>、<b>></b>、<b><</b>，如 <code>=200</code> 表示响应码等于200</li>\n<li><b>Contains</b>: 断言响应体包含指定内容，输入要查找的字符串即可</li>\n<li><b>JSONPath</b>: 断言响应体通过 JSONPath 表达式提取的值等于对比值，支持如 <code>$.data[0].id</code> 语法</li>\n</ul>\n</html>");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN, 12f));
        add(helpLabel, gbc);

        typeCombo.addActionListener(e -> updateFieldVisibility());
        updateFieldVisibility();
    }

    private void updateFieldVisibility() {
        String type = (String) typeCombo.getSelectedItem();
        inputCardLayout.show(inputPanel, type);
    }

    public void setAssertionData(JMeterTreeNode node) {
        this.currentNode = node;
        AssertionData data = node.assertionData;
        if (data == null) {
            data = new AssertionData();
            node.assertionData = data;
        }
        typeCombo.setSelectedItem(data.type);
        if ("Response Code".equals(data.type)) {
            operatorCombo.setSelectedItem(data.operator);
            responseCodeValueField.setText(data.value);
        } else if ("Contains".equals(data.type)) {
            containsContentField.setText(data.content);
        } else if ("JSONPath".equals(data.type)) {
            jsonPathField.setText(data.value);
            jsonPathExpectField.setText(data.content);
        }
        updateFieldVisibility();
    }

    public void saveAssertionData() {
        if (currentNode == null) return;
        AssertionData data = currentNode.assertionData;
        if (data == null) {
            data = new AssertionData();
            currentNode.assertionData = data;
        }
        data.type = (String) typeCombo.getSelectedItem();
        if ("Response Code".equals(data.type)) {
            data.operator = (String) operatorCombo.getSelectedItem();
            data.value = responseCodeValueField.getText();
            data.content = "";
        } else if ("Contains".equals(data.type)) {
            data.content = containsContentField.getText();
            data.operator = "=";
            data.value = "";
        } else if ("JSONPath".equals(data.type)) {
            data.value = jsonPathField.getText();
            data.content = jsonPathExpectField.getText();
            data.operator = "=";
        }
    }
}
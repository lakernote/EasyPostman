package com.laker.postman.panel.jmeter;

import javax.swing.*;
import java.awt.*;

public class AssertionPropertyPanel extends JPanel {
    private final JComboBox<String> typeCombo;
    private final JTextField contentField;
    private JMeterTreeNode currentNode;

    AssertionPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(420, 100));
        setPreferredSize(new Dimension(380, 80));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("断言类型:"), gbc);
        gbc.gridx = 1;
        typeCombo = new JComboBox<>(new String[]{"响应码", "响应体包含", "JSON路径"});
        add(typeCombo, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("断言内容:"), gbc);
        gbc.gridx = 1;
        contentField = new JTextField();
        add(contentField, gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), gbc);
    }

    public void setAssertionData(JMeterTreeNode node) {
        this.currentNode = node;
        AssertionData data = node.assertionData;
        if (data == null) {
            data = new AssertionData();
            node.assertionData = data;
        }
        typeCombo.setSelectedItem(data.type);
        contentField.setText(data.content);
    }

    public void saveAssertionData() {
        if (currentNode == null) return;
        AssertionData data = currentNode.assertionData;
        if (data == null) {
            data = new AssertionData();
            currentNode.assertionData = data;
        }
        data.type = (String) typeCombo.getSelectedItem();
        data.content = contentField.getText();
    }
}
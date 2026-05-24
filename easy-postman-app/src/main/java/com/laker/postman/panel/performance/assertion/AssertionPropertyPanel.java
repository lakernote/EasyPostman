package com.laker.postman.panel.performance.assertion;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

public class AssertionPropertyPanel extends JPanel {
    private final JComboBox<AssertionType> typeCombo;
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

    public AssertionPropertyPanel() {
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
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_TYPE_LABEL)), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        typeCombo = new JComboBox<>(AssertionType.values());
        typeCombo.setRenderer(new AssertionTypeRenderer());
        add(typeCombo, gbc);

        // 输入区采用CardLayout
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
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
        responseCodePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_OPERATOR)), rcGbc);
        rcGbc.gridx = 1;
        operatorCombo = new JComboBox<>(new String[]{"=", ">", "<"});
        responseCodePanel.add(operatorCombo, rcGbc);
        rcGbc.gridx = 2;
        responseCodePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_VALUE)), rcGbc);
        rcGbc.gridx = 3;
        responseCodeValueField = new JTextField(6);
        responseCodePanel.add(responseCodeValueField, rcGbc);

        // Contains面板
        containsPanel = new JPanel(new BorderLayout(2, 2));
        containsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_CONTAINS_CONTENT)), BorderLayout.WEST);
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
        jsonPathPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_JSONPATH)), jpGbc);
        jpGbc.gridx = 1;
        jpGbc.weightx = 1.0; // 让文本框填满剩余空间
        jpGbc.fill = GridBagConstraints.HORIZONTAL; // 水平填充
        jsonPathField = new JTextField(10);
        jsonPathPanel.add(jsonPathField, jpGbc);
        jpGbc.gridx = 0;
        jpGbc.gridy = 1;
        jpGbc.weightx = 0;
        jpGbc.fill = GridBagConstraints.HORIZONTAL;
        jsonPathPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_EXPECTED_VALUE)), jpGbc);
        jpGbc.gridx = 1;
        jpGbc.weightx = 1.0;
        jpGbc.fill = GridBagConstraints.HORIZONTAL;
        jsonPathExpectField = new JTextField();
        jsonPathPanel.add(jsonPathExpectField, jpGbc);

        inputPanel.add(responseCodePanel, AssertionType.RESPONSE_CODE.getStorageValue());
        inputPanel.add(containsPanel, AssertionType.CONTAINS.getStorageValue());
        inputPanel.add(jsonPathPanel, AssertionType.JSON_PATH.getStorageValue());
        add(inputPanel, gbc);

        // 帮助说明
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel helpLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_HELP));
        helpLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        add(helpLabel, gbc);

        typeCombo.addActionListener(e -> updateFieldVisibility());
        updateFieldVisibility();
    }

    private void updateFieldVisibility() {
        AssertionType type = (AssertionType) typeCombo.getSelectedItem();
        inputCardLayout.show(inputPanel, (type == null ? AssertionType.RESPONSE_CODE : type).getStorageValue());
    }

    public void setAssertionData(JMeterTreeNode node) {
        this.currentNode = node;
        AssertionData data = node.assertionData;
        if (data == null) {
            data = new AssertionData();
            node.assertionData = data;
        }
        AssertionType type = AssertionType.fromStorageValue(data.type);
        typeCombo.setSelectedItem(type);
        if (type == AssertionType.RESPONSE_CODE) {
            operatorCombo.setSelectedItem(data.operator);
            responseCodeValueField.setText(data.value);
        } else if (type == AssertionType.CONTAINS) {
            containsContentField.setText(data.content);
        } else if (type == AssertionType.JSON_PATH) {
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
        AssertionType type = (AssertionType) typeCombo.getSelectedItem();
        type = type == null ? AssertionType.RESPONSE_CODE : type;
        data.type = type.getStorageValue();
        if (type == AssertionType.RESPONSE_CODE) {
            data.operator = (String) operatorCombo.getSelectedItem();
            data.value = responseCodeValueField.getText();
            data.content = "";
        } else if (type == AssertionType.CONTAINS) {
            data.content = containsContentField.getText();
            data.operator = "=";
            data.value = "";
        } else if (type == AssertionType.JSON_PATH) {
            data.value = jsonPathField.getText();
            data.content = jsonPathExpectField.getText();
            data.operator = "=";
        }
    }

    private static final class AssertionTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AssertionType assertionType) {
                setText(assertionType.displayName());
            }
            return component;
        }
    }
}

package com.laker.postman.panel.performance.assertion;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class AssertionPropertyPanel extends JPanel {
    private static final String CARD_NUMERIC = "numeric";
    private static final String CARD_CONTAINS = "contains";
    private static final String CARD_JSON_PATH = "jsonPath";
    private static final String CARD_REGEX = "regex";
    private static final String CARD_HEADER_EXISTS = "headerExists";
    private static final String CARD_HEADER_EQUALS = "headerEquals";

    private final JComboBox<AssertionType> typeCombo;
    private final JComboBox<String> operatorCombo;
    private final JTextField numericValueField;
    private final JLabel numericValueLabel;
    private final JTextField containsContentField;
    private final JTextField jsonPathField;
    private final JTextField jsonPathExpectField;
    private final JTextField regexField;
    private final JTextField headerExistsNameField;
    private final JTextField headerEqualsNameField;
    private final JTextField headerEqualsValueField;
    private final CardLayout inputCardLayout;
    private final JPanel inputPanel;
    private JMeterTreeNode currentNode;

    public AssertionPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(520, 230));
        setPreferredSize(new Dimension(460, 200));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        typeCombo = new JComboBox<>(AssertionType.values());
        typeCombo.setRenderer(new AssertionTypeRenderer());
        operatorCombo = new JComboBox<>(new String[]{"=", ">", "<", ">=", "<=", "!="});
        numericValueField = new JTextField(10);
        numericValueLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_VALUE));
        containsContentField = new JTextField();
        jsonPathField = new JTextField();
        jsonPathExpectField = new JTextField();
        regexField = new JTextField();
        headerExistsNameField = new JTextField();
        headerEqualsNameField = new JTextField();
        headerEqualsValueField = new JTextField();

        addRow(0, I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_TYPE_LABEL), typeCombo);

        inputCardLayout = new CardLayout();
        inputPanel = new JPanel(inputCardLayout);
        inputPanel.add(createNumericPanel(), CARD_NUMERIC);
        inputPanel.add(createContainsPanel(), CARD_CONTAINS);
        inputPanel.add(createJsonPathPanel(), CARD_JSON_PATH);
        inputPanel.add(createRegexPanel(), CARD_REGEX);
        inputPanel.add(createHeaderExistsPanel(), CARD_HEADER_EXISTS);
        inputPanel.add(createHeaderEqualsPanel(), CARD_HEADER_EQUALS);

        GridBagConstraints inputGbc = baseGbc(0, 1);
        inputGbc.gridwidth = 2;
        inputGbc.weightx = 1.0;
        add(inputPanel, inputGbc);

        GridBagConstraints helpGbc = baseGbc(0, 2);
        helpGbc.gridwidth = 2;
        JLabel helpLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_HELP));
        helpLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        add(helpLabel, helpGbc);

        typeCombo.addActionListener(e -> updateTypeState());
        updateTypeState();
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
        operatorCombo.setSelectedItem(data.operator);
        numericValueField.setText(data.value);
        containsContentField.setText(data.content);
        jsonPathField.setText(data.value);
        jsonPathExpectField.setText(data.content);
        regexField.setText(data.content);
        headerExistsNameField.setText(data.content);
        headerEqualsNameField.setText(data.content);
        headerEqualsValueField.setText(data.value);
        updateTypeState();
    }

    public void saveAssertionData() {
        if (currentNode == null) {
            return;
        }
        AssertionData data = currentNode.assertionData;
        if (data == null) {
            data = new AssertionData();
            currentNode.assertionData = data;
        }
        AssertionType type = (AssertionType) typeCombo.getSelectedItem();
        type = type == null ? AssertionType.RESPONSE_CODE : type;
        data.type = type.getStorageValue();
        data.operator = (String) operatorCombo.getSelectedItem();
        switch (type) {
            case RESPONSE_CODE, RESPONSE_TIME, BODY_SIZE -> {
                data.value = numericValueField.getText();
                data.content = "";
            }
            case CONTAINS -> {
                data.content = containsContentField.getText();
                data.value = "";
                data.operator = "=";
            }
            case JSON_PATH -> {
                data.value = jsonPathField.getText();
                data.content = jsonPathExpectField.getText();
                data.operator = "=";
            }
            case REGEX -> {
                data.content = regexField.getText();
                data.value = "";
                data.operator = "=";
            }
            case HEADER_EXISTS -> {
                data.content = headerExistsNameField.getText();
                data.value = "";
                data.operator = "=";
            }
            case HEADER_EQUALS -> {
                data.content = headerEqualsNameField.getText();
                data.value = headerEqualsValueField.getText();
                data.operator = "=";
            }
        }
    }

    private void updateTypeState() {
        AssertionType type = (AssertionType) typeCombo.getSelectedItem();
        type = type == null ? AssertionType.RESPONSE_CODE : type;
        switch (type) {
            case RESPONSE_CODE -> {
                numericValueLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_VALUE));
                numericValueField.putClientProperty(
                        FlatClientProperties.PLACEHOLDER_TEXT,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_STATUS)
                );
                inputCardLayout.show(inputPanel, CARD_NUMERIC);
            }
            case RESPONSE_TIME -> {
                numericValueLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_RESPONSE_TIME_MS));
                numericValueField.putClientProperty(
                        FlatClientProperties.PLACEHOLDER_TEXT,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_TIME)
                );
                inputCardLayout.show(inputPanel, CARD_NUMERIC);
            }
            case BODY_SIZE -> {
                numericValueLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_BODY_SIZE_BYTES));
                numericValueField.putClientProperty(
                        FlatClientProperties.PLACEHOLDER_TEXT,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_BODY_SIZE)
                );
                inputCardLayout.show(inputPanel, CARD_NUMERIC);
            }
            case CONTAINS -> inputCardLayout.show(inputPanel, CARD_CONTAINS);
            case JSON_PATH -> inputCardLayout.show(inputPanel, CARD_JSON_PATH);
            case REGEX -> inputCardLayout.show(inputPanel, CARD_REGEX);
            case HEADER_EXISTS -> inputCardLayout.show(inputPanel, CARD_HEADER_EXISTS);
            case HEADER_EQUALS -> inputCardLayout.show(inputPanel, CARD_HEADER_EQUALS);
        }
    }

    private JPanel createNumericPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        addToPanel(panel, 0, 0, new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_OPERATOR)), 0);
        addToPanel(panel, 1, 0, operatorCombo, 0);
        addToPanel(panel, 2, 0, numericValueLabel, 0);
        addToPanel(panel, 3, 0, numericValueField, 1.0);
        return panel;
    }

    private JPanel createContainsPanel() {
        containsContentField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_CONTAINS)
        );
        return createSingleFieldPanel(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_CONTAINS_CONTENT),
                containsContentField
        );
    }

    private JPanel createJsonPathPanel() {
        jsonPathField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_JSONPATH)
        );
        jsonPathExpectField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_EXPECTED)
        );
        JPanel panel = new JPanel(new GridBagLayout());
        addToPanel(panel, 0, 0, new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_JSONPATH)), 0);
        addToPanel(panel, 1, 0, jsonPathField, 1.0);
        addToPanel(panel, 0, 1, new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_EXPECTED_VALUE)), 0);
        addToPanel(panel, 1, 1, jsonPathExpectField, 1.0);
        return panel;
    }

    private JPanel createRegexPanel() {
        regexField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_REGEX)
        );
        return createSingleFieldPanel(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_REGEX),
                regexField
        );
    }

    private JPanel createHeaderExistsPanel() {
        headerExistsNameField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_HEADER)
        );
        return createSingleFieldPanel(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_HEADER_NAME),
                headerExistsNameField
        );
    }

    private JPanel createHeaderEqualsPanel() {
        headerEqualsNameField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_HEADER)
        );
        headerEqualsValueField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_PLACEHOLDER_EXPECTED)
        );
        JPanel panel = new JPanel(new GridBagLayout());
        addToPanel(panel, 0, 0, new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_HEADER_NAME)), 0);
        addToPanel(panel, 1, 0, headerEqualsNameField, 1.0);
        addToPanel(panel, 0, 1, new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_EXPECTED_VALUE)), 0);
        addToPanel(panel, 1, 1, headerEqualsValueField, 1.0);
        return panel;
    }

    private JPanel createSingleFieldPanel(String labelText, Component field) {
        JPanel panel = new JPanel(new GridBagLayout());
        addToPanel(panel, 0, 0, new JLabel(labelText), 0);
        addToPanel(panel, 1, 0, field, 1.0);
        return panel;
    }

    private void addRow(int row, String labelText, Component component) {
        add(new JLabel(labelText), baseGbc(0, row));
        GridBagConstraints gbc = baseGbc(1, row);
        gbc.weightx = 1.0;
        add(component, gbc);
    }

    private void addToPanel(JPanel panel, int x, int y, Component component, double weightx) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = weightx;
        panel.add(component, gbc);
    }

    private GridBagConstraints baseGbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
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

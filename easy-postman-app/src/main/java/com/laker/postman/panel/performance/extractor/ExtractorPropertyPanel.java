package com.laker.postman.panel.performance.extractor;

import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.extractor.ExtractorType;


import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.panel.performance.model.PerformanceTreeNode;
import com.laker.postman.panel.performance.tree.PerformanceTreeNodeTitleFormatter;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ExtractorPropertyPanel extends JPanel {
    private final JComboBox<ExtractorType> typeCombo;
    private final JTextField expressionField;
    private final JTextField variableNameField;
    private final JTextField defaultValueField;
    private final JSpinner matchIndexSpinner;
    private final JSpinner groupIndexSpinner;
    private final JLabel expressionLabel;
    private final JLabel groupIndexLabel;
    private PerformanceTreeNode currentNode;

    public ExtractorPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(520, 220));
        setPreferredSize(new Dimension(460, 190));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        typeCombo = new JComboBox<>(ExtractorType.values());
        typeCombo.setRenderer(new ExtractorTypeRenderer());
        expressionField = new JTextField();
        variableNameField = new JTextField();
        defaultValueField = new JTextField();
        matchIndexSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        groupIndexSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 99, 1));
        expressionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_EXPRESSION));
        groupIndexLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_GROUP_INDEX));

        int row = 0;
        addRow(row++, I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_TYPE_LABEL), typeCombo);
        addRow(row++, I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_VARIABLE_NAME), variableNameField);
        addRow(row++, expressionLabel, expressionField);
        addRow(row++, I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_DEFAULT_VALUE), defaultValueField);
        addRow(row, I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_MATCH_INDEX), matchIndexSpinner);
        addAt(row, 2, groupIndexLabel);
        addAt(row, 3, groupIndexSpinner);

        GridBagConstraints helpGbc = baseGbc(0, row + 1);
        helpGbc.gridwidth = 4;
        JLabel helpLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_HELP));
        helpLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        add(helpLabel, helpGbc);

        typeCombo.addActionListener(e -> updateTypeState());
        updateTypeState();
    }

    public void setExtractorData(PerformanceTreeNode node) {
        this.currentNode = node;
        ExtractorData data = node.extractorData;
        if (data == null) {
            data = new ExtractorData();
            node.extractorData = data;
        }
        typeCombo.setSelectedItem(ExtractorType.fromStorageValue(data.type));
        expressionField.setText(data.expression);
        variableNameField.setText(data.variableName);
        defaultValueField.setText(data.defaultValue);
        matchIndexSpinner.setValue(Math.max(1, data.matchIndex));
        groupIndexSpinner.setValue(Math.max(0, data.groupIndex));
        updateTypeState();
    }

    public void saveExtractorData() {
        if (currentNode == null) {
            return;
        }
        ExtractorData data = currentNode.extractorData;
        if (data == null) {
            data = new ExtractorData();
            currentNode.extractorData = data;
        }
        ExtractorType type = (ExtractorType) typeCombo.getSelectedItem();
        type = type == null ? ExtractorType.JSON_PATH : type;
        data.type = type.getStorageValue();
        data.expression = expressionField.getText();
        data.variableName = variableNameField.getText();
        data.defaultValue = defaultValueField.getText();
        data.matchIndex = ((Number) matchIndexSpinner.getValue()).intValue();
        data.groupIndex = ((Number) groupIndexSpinner.getValue()).intValue();
        currentNode.name = PerformanceTreeNodeTitleFormatter.extractorTitle(data);
    }

    public void forceCommitAllSpinners() {
        try {
            matchIndexSpinner.commitEdit();
            groupIndexSpinner.commitEdit();
        } catch (Exception ignored) {
            // Keep current spinner value if the user is mid-edit.
        }
    }

    private void updateTypeState() {
        ExtractorType type = (ExtractorType) typeCombo.getSelectedItem();
        type = type == null ? ExtractorType.JSON_PATH : type;
        expressionLabel.setText(switch (type) {
            case JSON_PATH -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_JSONPATH);
            case REGEX -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_REGEX);
            case HEADER -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_HEADER_NAME);
            case COOKIE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_COOKIE_NAME);
        });
        expressionField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, switch (type) {
            case JSON_PATH -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_PLACEHOLDER_JSONPATH);
            case REGEX -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_PLACEHOLDER_REGEX);
            case HEADER -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_PLACEHOLDER_HEADER);
            case COOKIE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_PLACEHOLDER_COOKIE);
        });
        variableNameField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_PLACEHOLDER_VARIABLE)
        );
        defaultValueField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_PLACEHOLDER_DEFAULT)
        );
        boolean regex = type == ExtractorType.REGEX;
        groupIndexLabel.setEnabled(regex);
        groupIndexSpinner.setEnabled(regex);
    }

    private void addRow(int row, String labelText, Component component) {
        addRow(row, new JLabel(labelText), component);
    }

    private void addRow(int row, JLabel label, Component component) {
        addAt(row, 0, label);
        GridBagConstraints gbc = baseGbc(1, row);
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        add(component, gbc);
    }

    private void addAt(int row, int column, Component component) {
        add(component, baseGbc(column, row));
    }

    private GridBagConstraints baseGbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private static final class ExtractorTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ExtractorType extractorType) {
                setText(I18nUtil.getMessage(extractorType.getMessageKey()));
            }
            return component;
        }
    }
}

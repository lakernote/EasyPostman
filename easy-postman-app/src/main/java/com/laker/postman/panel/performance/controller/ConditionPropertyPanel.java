package com.laker.postman.panel.performance.controller;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.panel.performance.PerformanceStagePropertyLayout;
import com.laker.postman.panel.performance.tree.PerformanceTreeNodeTitleFormatter;
import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

public class ConditionPropertyPanel extends JPanel {
    private final JTextField expressionField;
    private PerformanceTreeNode currentNode;

    public ConditionPropertyPanel() {
        setLayout(new GridBagLayout());
        PerformanceStagePropertyLayout.applyCompactBorder(this);

        GridBagConstraints gbc = PerformanceStagePropertyLayout.createBaseConstraints();
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CONDITION_EXPRESSION));
        expressionField = new JTextField();
        expressionField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_CONDITION_PLACEHOLDER)
        );
        PerformanceStagePropertyLayout.configureFieldWidth(
                expressionField,
                PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH,
                180
        );
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(this, gbc, label, expressionField);
        PerformanceStagePropertyLayout.addCenteredHintArea(
                this,
                gbc,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_CONDITION_DESCRIPTION),
                7,
                72
        );

        PerformanceStagePropertyLayout.addVerticalFiller(this, gbc, 2);
    }

    public void setConditionData(PerformanceTreeNode node) {
        currentNode = node;
        ConditionData data = resolveConditionData(node);
        expressionField.setText(data.expression);
    }

    public void saveConditionData() {
        if (currentNode == null) {
            return;
        }
        ConditionData data = resolveConditionData(currentNode);
        data.expression = expressionField.getText();
        data.normalize();
        currentNode.name = PerformanceTreeNodeTitleFormatter.conditionTitle(data);
    }

    public void forceCommitAllSpinners() {
        // No spinner in this panel; kept for property-panel commit symmetry.
    }

    private ConditionData resolveConditionData(PerformanceTreeNode node) {
        ConditionData data = node.conditionData;
        if (data == null) {
            data = new ConditionData();
            node.conditionData = data;
        }
        data.normalize();
        return data;
    }
}

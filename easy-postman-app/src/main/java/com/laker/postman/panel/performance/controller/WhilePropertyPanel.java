package com.laker.postman.panel.performance.controller;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.panel.performance.PerformanceStagePropertyLayout;
import com.laker.postman.panel.performance.tree.PerformanceTreeNodeTitleFormatter;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

public class WhilePropertyPanel extends JPanel {
    private final JTextField expressionField;
    private final EasyJSpinner intervalSpinner;
    private final EasyJSpinner timeoutSpinner;
    private final EasyJSpinner maxIterationsSpinner;
    private PerformanceTreeNode currentNode;

    public WhilePropertyPanel() {
        setLayout(new GridBagLayout());
        PerformanceStagePropertyLayout.applyCompactBorder(this);

        GridBagConstraints gbc = PerformanceStagePropertyLayout.createBaseConstraints();
        expressionField = new JTextField();
        expressionField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WHILE_PLACEHOLDER)
        );
        PerformanceStagePropertyLayout.configureFieldWidth(
                expressionField,
                PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH,
                180
        );
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(
                this,
                gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WHILE_EXPRESSION)),
                expressionField
        );

        intervalSpinner = createSpinner(WhileData.MIN_INTERVAL_MS, WhileData.MAX_INTERVAL_MS, 100);
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(
                this,
                gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WHILE_INTERVAL)),
                intervalSpinner
        );

        timeoutSpinner = createSpinner(WhileData.MIN_TIMEOUT_MS, WhileData.MAX_TIMEOUT_MS, 1000);
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(
                this,
                gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WHILE_TIMEOUT)),
                timeoutSpinner
        );

        maxIterationsSpinner = createSpinner(WhileData.MIN_MAX_ITERATIONS, WhileData.MAX_MAX_ITERATIONS, 1);
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(
                this,
                gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WHILE_MAX_ITERATIONS)),
                maxIterationsSpinner
        );

        PerformanceStagePropertyLayout.addCenteredHintArea(
                this,
                gbc,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WHILE_DESCRIPTION),
                8,
                74
        );
        PerformanceStagePropertyLayout.addVerticalFiller(this, gbc, 2);
    }

    public void setWhileData(PerformanceTreeNode node) {
        currentNode = node;
        WhileData data = resolveWhileData(node);
        expressionField.setText(data.expression);
        intervalSpinner.setValue(data.intervalMs);
        timeoutSpinner.setValue(data.timeoutMs);
        maxIterationsSpinner.setValue(data.maxIterations);
    }

    public void saveWhileData() {
        if (currentNode == null) {
            return;
        }
        forceCommitAllSpinners();
        WhileData data = resolveWhileData(currentNode);
        data.expression = expressionField.getText();
        data.intervalMs = intervalSpinner.getCommittedIntValue();
        data.timeoutMs = timeoutSpinner.getCommittedIntValue();
        data.maxIterations = maxIterationsSpinner.getCommittedIntValue();
        data.normalize();
        currentNode.name = PerformanceTreeNodeTitleFormatter.whileTitle(data);
    }

    public void forceCommitAllSpinners() {
        intervalSpinner.forceCommit();
        timeoutSpinner.forceCommit();
        maxIterationsSpinner.forceCommit();
    }

    private EasyJSpinner createSpinner(int min, int max, int step) {
        EasyJSpinner spinner = EasyJSpinner.intSpinner(min, min, max, step);
        PerformanceStagePropertyLayout.configureFieldWidth(
                spinner,
                PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH,
                96
        );
        return spinner;
    }

    private WhileData resolveWhileData(PerformanceTreeNode node) {
        WhileData data = node.whileData;
        if (data == null) {
            data = new WhileData();
            node.whileData = data;
        }
        data.normalize();
        return data;
    }
}

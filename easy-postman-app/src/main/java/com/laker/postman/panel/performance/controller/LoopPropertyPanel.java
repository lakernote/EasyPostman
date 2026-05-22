package com.laker.postman.panel.performance.controller;

import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.panel.performance.PerformanceStagePropertyLayout;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

public class LoopPropertyPanel extends JPanel {
    private final EasyJSpinner iterationsSpinner;
    private JMeterTreeNode currentNode;

    public LoopPropertyPanel() {
        setLayout(new GridBagLayout());
        PerformanceStagePropertyLayout.applyCompactBorder(this);

        GridBagConstraints gbc = PerformanceStagePropertyLayout.createBaseConstraints();
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_LOOP_ITERATIONS));
        iterationsSpinner = new EasyJSpinner(new SpinnerNumberModel(
                LoopData.MIN_ITERATIONS,
                LoopData.MIN_ITERATIONS,
                LoopData.MAX_ITERATIONS,
                1
        ));
        PerformanceStagePropertyLayout.configureFieldWidth(
                iterationsSpinner,
                PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH,
                96
        );
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(this, gbc, label, iterationsSpinner);

        JLabel hintLabel = new JLabel("<html><span style='color:gray'>"
                + I18nUtil.getMessage(MessageKeys.PERFORMANCE_LOOP_HINT)
                + "</span></html>");
        hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(6, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(hintLabel, gbc);
        gbc.gridy++;

        PerformanceStagePropertyLayout.addVerticalFiller(this, gbc, 2);
    }

    public void setLoopData(JMeterTreeNode node) {
        currentNode = node;
        LoopData data = resolveLoopData(node);
        iterationsSpinner.setValue(data.iterations);
    }

    public void saveLoopData() {
        if (currentNode == null) {
            return;
        }
        LoopData data = resolveLoopData(currentNode);
        data.iterations = (Integer) iterationsSpinner.getValue();
        data.normalize();
        currentNode.name = buildLoopTitle(data);
    }

    public void forceCommitAllSpinners() {
        iterationsSpinner.forceCommit();
    }

    private LoopData resolveLoopData(JMeterTreeNode node) {
        LoopData data = node.loopData;
        if (data == null) {
            data = new LoopData();
            node.loopData = data;
        }
        data.normalize();
        return data;
    }

    private String buildLoopTitle(LoopData data) {
        return I18nUtil.getMessage(MessageKeys.PERFORMANCE_LOOP_NODE)
                + " [" + data.iterations + "x]";
    }
}

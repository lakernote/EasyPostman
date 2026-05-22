package com.laker.postman.panel.performance.controller;

import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.panel.performance.PerformanceStagePropertyLayout;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
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
        iterationsSpinner = EasyJSpinner.intSpinner(
                LoopData.MIN_ITERATIONS,
                LoopData.MIN_ITERATIONS,
                LoopData.MAX_ITERATIONS,
                1
        );
        PerformanceStagePropertyLayout.configureFieldWidth(
                iterationsSpinner,
                PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH,
                96
        );
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(this, gbc, label, iterationsSpinner);

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
        data.iterations = iterationsSpinner.getCommittedIntValue();
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

package com.laker.postman.panel.jmeter;

import javax.swing.*;
import java.awt.*;

public class TimerPropertyPanel extends JPanel {
    private final JSpinner delaySpinner;
    private JMeterTreeNode currentNode;

    TimerPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(420, 60));
        setPreferredSize(new Dimension(380, 48));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("定时(ms):"), gbc);
        gbc.gridx = 1;
        delaySpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 60000, 100));
        add(delaySpinner, gbc);
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), gbc);
    }

    public void setTimerData(JMeterTreeNode node) {
        this.currentNode = node;
        TimerData data = node.timerData;
        if (data == null) {
            data = new TimerData();
            node.timerData = data;
        }
        delaySpinner.setValue(data.delayMs);
    }

    public void saveTimerData() {
        if (currentNode == null) return;
        TimerData data = currentNode.timerData;
        if (data == null) {
            data = new TimerData();
            currentNode.timerData = data;
        }
        data.delayMs = (Integer) delaySpinner.getValue();
    }

    public void requestFocusInField() {
        delaySpinner.requestFocusInWindow();
    }
}

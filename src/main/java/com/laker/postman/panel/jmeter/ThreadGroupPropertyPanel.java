package com.laker.postman.panel.jmeter;

import javax.swing.*;
import java.awt.*;

public class ThreadGroupPropertyPanel extends JPanel {
    private final JSpinner numThreadsSpinner;
    private final JSpinner loopsSpinner;
    private JMeterTreeNode currentNode;

    ThreadGroupPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(420, 160));
        setPreferredSize(new Dimension(380, 120));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("用户数:"), gbc);
        gbc.gridx = 1;
        numThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        add(numThreadsSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("循环次数:"), gbc);
        gbc.gridx = 1;
        loopsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        add(loopsSpinner, gbc);
        add(Box.createVerticalGlue(), gbc);
    }

    // 回填数据
    public void setThreadGroupData(JMeterTreeNode node) {
        this.currentNode = node;
        ThreadGroupData data = node.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            node.threadGroupData = data;
        }
        numThreadsSpinner.setValue(data.numThreads);
        loopsSpinner.setValue(data.loops);
    }

    public void saveThreadGroupData() {
        if (currentNode == null) return;
        ThreadGroupData data = currentNode.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            currentNode.threadGroupData = data;
        }
        data.numThreads = (Integer) numThreadsSpinner.getValue();
        data.loops = (Integer) loopsSpinner.getValue();
    }
}
package com.laker.postman.panel.jmeter;

import javax.swing.*;
import java.awt.*;

public class TimerPropertyPanel extends JPanel {
    private final JSpinner delaySpinner;
    private JMeterTreeNode currentNode;

    TimerPropertyPanel() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(420, 120));
        setPreferredSize(new Dimension(380, 100));
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel label = new JLabel("定时(ms):");
        add(label, gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(6, 0, 6, 6); // 左间距为0，右间距为6
        delaySpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 60000, 100));
        delaySpinner.setPreferredSize(new Dimension(100, 28));
        add(delaySpinner, gbc);
        // 帮助说明
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel helpLabel = new JLabel("<html><span style='color:gray'>定时器会在请求前延迟指定毫秒数，适用于接口限流、节奏控制等场景。</span></html>");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN, 12f));
        add(helpLabel, gbc);
        // 占位撑满高度
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 2;
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

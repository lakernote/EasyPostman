package com.laker.postman.panel.performance.timer;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

public class TimerPropertyPanel extends JPanel {
    private final JSpinner delaySpinner;
    private JMeterTreeNode currentNode;

    public TimerPropertyPanel() {
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
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TIMER_DELAY));
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
        JLabel helpLabel = new JLabel("<html><span style='color:gray'>"
                + I18nUtil.getMessage(MessageKeys.PERFORMANCE_TIMER_HINT)
                + "</span></html>");
        helpLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
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
}

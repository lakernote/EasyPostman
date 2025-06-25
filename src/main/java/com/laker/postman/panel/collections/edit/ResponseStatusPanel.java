package com.laker.postman.panel.collections.edit;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * 状态栏面板，展示状态码、响应时间、响应大小
 */
@Getter
public class ResponseStatusPanel extends JPanel {
    private final JLabel statusCodeLabel;
    private final JLabel responseTimeLabel;
    private final JLabel responseSizeLabel;

    public ResponseStatusPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusCodeLabel = new JLabel("Status: --");
        responseTimeLabel = new JLabel("Duration: --");
        responseSizeLabel = new JLabel("ResponseSize: --");
        add(statusCodeLabel);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(responseTimeLabel);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(responseSizeLabel);
    }
}
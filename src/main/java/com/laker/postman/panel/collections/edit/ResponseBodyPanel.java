package com.laker.postman.panel.collections.edit;

import javax.swing.*;
import java.awt.*;

/**
 * 响应体面板，展示响应体内容和格式化按钮
 */
public class ResponseBodyPanel extends JPanel {
    private final JTextPane responseBodyPane;
    private final JButton formatButton;

    public ResponseBodyPanel() {
        setLayout(new BorderLayout());
        responseBodyPane = new JTextPane();
        responseBodyPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(responseBodyPane);
        add(scrollPane, BorderLayout.CENTER);
        formatButton = new JButton("Format");
        add(formatButton, BorderLayout.SOUTH);
    }

    public JTextPane getResponseBodyPane() {
        return responseBodyPane;
    }

    public JButton getFormatButton() {
        return formatButton;
    }

    public void setBodyText(String text) {
        responseBodyPane.setText(text);
        responseBodyPane.setCaretPosition(0);
    }
}
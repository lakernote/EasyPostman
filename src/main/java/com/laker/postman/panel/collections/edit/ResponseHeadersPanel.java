package com.laker.postman.panel.collections.edit;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * 响应头面板，只读展示响应头内容
 */
@Getter
public class ResponseHeadersPanel extends JPanel {
    private final JTextArea responseHeadersArea;

    public ResponseHeadersPanel() {
        setLayout(new BorderLayout());
        responseHeadersArea = new JTextArea(5, 20);
        responseHeadersArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(responseHeadersArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setHeadersText(String text) {
        responseHeadersArea.setText(text);
        responseHeadersArea.setCaretPosition(0);
    }

}
package com.laker.postman.panel.collections.edit;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * 顶部请求行面板，包含方法选择、URL输入、发送按钮
 */
public class RequestLinePanel extends JPanel {
    private final JComboBox<String> methodBox;
    private final JTextField urlField;
    private final JButton sendButton;
    private final JCheckBox followRedirectsCheckBox;

    public RequestLinePanel(ActionListener sendAction) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"};
        methodBox = new JComboBox<>(methods);
        add(methodBox);
        add(Box.createHorizontalStrut(2));
        urlField = new JTextField("https://www.baidu.com", 25);
        add(urlField);
        add(Box.createHorizontalStrut(10));
        sendButton = new JButton("Send");
        sendButton.addActionListener(sendAction);
        add(sendButton);
        add(Box.createHorizontalStrut(10));
        followRedirectsCheckBox = new JCheckBox("Follow Redirects", true);
        add(followRedirectsCheckBox);
        add(Box.createHorizontalGlue());
    }

    public JComboBox<String> getMethodBox() {
        return methodBox;
    }

    public JTextField getUrlField() {
        return urlField;
    }

    public JButton getSendButton() {
        return sendButton;
    }

    public JCheckBox getFollowRedirectsCheckBox() {
        return followRedirectsCheckBox;
    }
}
package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 顶部请求行面板，包含方法选择、URL输入、发送按钮
 */
@Getter
public class RequestLinePanel extends JPanel {
    private final JComboBox<String> methodBox;
    private final JTextField urlField;
    private final JButton sendButton;
    private final Color defaultButtonColor;
    private final Color textColor;

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
        defaultButtonColor = sendButton.getBackground();
        textColor = sendButton.getForeground();
        sendButton.addActionListener(sendAction);
        add(sendButton);
        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("保存当前请求");
        saveButton.addActionListener(e -> {
            SingletonFactory.getInstance(RequestEditPanel.class).saveCurrentRequest();
        });
        add(Box.createHorizontalStrut(6));
        add(saveButton);
        add(Box.createHorizontalStrut(10));
        add(Box.createHorizontalGlue());
    }

    /**
     * 切换按钮为 Send 状态
     */
    public void setSendButtonToSend(ActionListener sendAction) {
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }
        sendButton.setText("Send");
        sendButton.setBackground(defaultButtonColor); // Postman浅蓝色
        sendButton.setForeground(textColor);
        sendButton.setEnabled(true);
        sendButton.addActionListener(sendAction);
    }

    /**
     * 切换按钮为 Cancel 状态
     */
    public void setSendButtonToCancel(ActionListener cancelAction) {
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }
        sendButton.setText("Cancel");
        sendButton.setBackground(new java.awt.Color(0xD9D9D9)); // Postman浅灰色
        sendButton.setForeground(new java.awt.Color(0x333333)); // 深灰文字
        sendButton.setEnabled(true);
        sendButton.addActionListener(cancelAction);
    }

    public void setSendButtonToClose(ActionListener cancelAction) {
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }
        sendButton.setText("Close");
        sendButton.setBackground(new java.awt.Color(0xD9D9D9)); // Postman浅灰色
        sendButton.setForeground(new java.awt.Color(0x333333)); // 深灰文字
        sendButton.setEnabled(true);
        sendButton.addActionListener(cancelAction);
    }
}
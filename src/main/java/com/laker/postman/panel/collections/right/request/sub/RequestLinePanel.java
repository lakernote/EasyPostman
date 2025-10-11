package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.EasyPostmanTextField;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 顶部请求行面板，包含方法选择、URL输入、发送按钮
 */
@Getter
public class RequestLinePanel extends JPanel {
    public static final int WIDTH = 18;
    private final JComboBox<String> methodBox;
    private final JTextField urlField;
    private final JButton sendButton;
    private final Color defaultButtonColor;
    private final Color textColor;
    private final RequestItemProtocolEnum protocol;

    public RequestLinePanel(ActionListener sendAction, RequestItemProtocolEnum protocol) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.protocol = protocol;
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"};
        methodBox = new JComboBox<>(methods);
        if (protocol.isWebSocketProtocol()) {
            // WebSocket协议：隐藏方法选择框，WebSocket都是GET
            methodBox.setVisible(false);
            methodBox.setSelectedItem("GET"); // 强制设置为GET
        } else {
            // HTTP协议：显示方法选择框
            methodBox.setVisible(true);
        }
        add(methodBox);
        add(Box.createHorizontalStrut(2));

        urlField = new EasyPostmanTextField(null, 25, I18nUtil.getMessage(MessageKeys.REQUEST_URL_PLACEHOLDER));
        add(urlField);
        add(Box.createHorizontalStrut(10));
        sendButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_SEND));
        if (protocol.isWebSocketProtocol()) {
            sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT));
            sendButton.setIcon(new FlatSVGIcon("icons/connect.svg", WIDTH, WIDTH));
        } else {
            sendButton.setIcon(new FlatSVGIcon("icons/send.svg", WIDTH, WIDTH));
        }
        sendButton.setIconTextGap(6); // 图标和文字之间的间距
        defaultButtonColor = sendButton.getBackground();
        textColor = sendButton.getForeground();
        sendButton.addActionListener(sendAction);
        add(sendButton);
        JButton saveButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE));
        saveButton.setIcon(new FlatSVGIcon("icons/save.svg", WIDTH, WIDTH));
        saveButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE_TOOLTIP));
        saveButton.setIconTextGap(6); // 图标和文字之间的间距
        saveButton.addActionListener(e -> SingletonFactory.getInstance(RequestEditPanel.class).saveCurrentRequest());
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
        if (protocol.isWebSocketProtocol()) {
            sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT));
            sendButton.setIcon(new FlatSVGIcon("icons/connect.svg", WIDTH, WIDTH));
        } else {
            sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_SEND));
            sendButton.setIcon(new FlatSVGIcon("icons/send.svg", WIDTH, WIDTH));
        }
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
        sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        sendButton.setIcon(new FlatSVGIcon("icons/cancel.svg", WIDTH, WIDTH));
        sendButton.setBackground(new java.awt.Color(0xD9D9D9)); // Postman浅灰色
        sendButton.setForeground(new java.awt.Color(0x333333)); // 深灰文字
        sendButton.setEnabled(true);
        sendButton.addActionListener(cancelAction);
    }

    public void setSendButtonToClose(ActionListener cancelAction) {
        for (ActionListener al : sendButton.getActionListeners()) {
            sendButton.removeActionListener(al);
        }
        sendButton.setText(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        sendButton.setIcon(new FlatSVGIcon("icons/close.svg", WIDTH, WIDTH));
        sendButton.setBackground(new Color(0xD9D9D9)); // Postman浅灰色
        sendButton.setForeground(new Color(0x333333)); // 深灰文字
        sendButton.setEnabled(true);
        sendButton.addActionListener(cancelAction);
    }
}
package com.laker.postman.common.component.button;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import javax.swing.*;
import java.awt.*;
/**
 * WebSocket 发送按钮
 * 用于发送 WebSocket 消息
 */
public class WebSocketSendButton extends JButton {
    public WebSocketSendButton() {
        super(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_SEND));
        setIcon(IconUtil.createThemed("icons/send.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
        setVisible(true);
    }
}

package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * WebSocket 定时发送按钮
 */
public class WebSocketTimedSendButton extends JButton {
    public WebSocketTimedSendButton() {
        setIcon(IconUtil.createThemed("icons/time.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        setToolTipText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_START));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}

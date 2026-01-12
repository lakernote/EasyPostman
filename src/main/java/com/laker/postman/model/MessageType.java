package com.laker.postman.model;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

// 消息类型
public enum MessageType {
    SENT(I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_SENT), IconUtil.createThemed("icons/ws-send.svg", 16, 16)),
    RECEIVED(I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_RECEIVED), new FlatSVGIcon("icons/ws-receive.svg", 16, 16)),
    CONNECTED(I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CONNECTED), new FlatSVGIcon("icons/ws-connect.svg", 16, 16)),
    CLOSED(I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CLOSED), IconUtil.createThemed("icons/ws-close.svg", 16, 16)),
    WARNING(I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_WARNING), IconUtil.createThemed("icons/warning.svg", 16, 16)),
    INFO(I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_INFO), IconUtil.createThemed("icons/ws-info.svg", 16, 16)),
    BINARY(I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_BINARY), IconUtil.createThemed("icons/binary.svg", 16, 16));
    public final String display;
    public final Icon icon;

    MessageType(String display, Icon icon) {
        this.display = display;
        this.icon = icon;
    }

}
package com.laker.postman.panel.collections.editor.request;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.stream.MessageType;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

public final class StreamMessageUiMetadata {
    private StreamMessageUiMetadata() {
    }

    public static String display(MessageType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case SENT -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_SENT);
            case RECEIVED -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_RECEIVED);
            case CONNECTED -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CONNECTED);
            case CLOSED -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CLOSED);
            case WARNING -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_WARNING);
            case INFO -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_INFO);
            case BINARY -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_BINARY);
        };
    }

    public static Icon icon(MessageType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case SENT -> IconUtil.createThemed("icons/ws-send.svg", 16, 16);
            case RECEIVED -> new FlatSVGIcon("icons/ws-receive.svg", 16, 16);
            case CONNECTED -> new FlatSVGIcon("icons/ws-connect.svg", 16, 16);
            case CLOSED -> IconUtil.createThemed("icons/ws-close.svg", 16, 16);
            case WARNING -> IconUtil.createThemed("icons/warning.svg", 16, 16);
            case INFO -> IconUtil.createThemed("icons/ws-info.svg", 16, 16);
            case BINARY -> IconUtil.createThemed("icons/binary.svg", 16, 16);
        };
    }
}

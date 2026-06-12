package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.stream.MessageType;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.*;

@UtilityClass
public class StreamMessageUiMetadata {

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
            case SENT -> IconUtil.createColored("icons/ws-send.svg", 16, 16, ModernColors.getPrimary());
            case RECEIVED -> IconUtil.createColored("icons/ws-receive.svg", 16, 16, ModernColors.getWarning());
            case CONNECTED -> IconUtil.createColored("icons/ws-connect.svg", 16, 16, ModernColors.getSuccess());
            case CLOSED -> IconUtil.createThemed("icons/ws-close.svg", 16, 16);
            case WARNING -> IconUtil.createColored("icons/warning.svg", 16, 16, ModernColors.getWarning());
            case INFO -> IconUtil.createColored("icons/ws-info.svg", 16, 16, ModernColors.getInfo());
            case BINARY -> IconUtil.createThemed("icons/binary.svg", 16, 16);
        };
    }
}

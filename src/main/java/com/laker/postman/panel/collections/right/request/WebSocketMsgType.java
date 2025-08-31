package com.laker.postman.panel.collections.right.request;

import com.laker.postman.util.MessageKeys;

/**
 * WebSocket消息类型及匹配逻辑
 */
public enum WebSocketMsgType {
    CONNECTED(MessageKeys.WS_ICON_CONNECTED),
    RECEIVED(MessageKeys.WS_ICON_RECEIVED),
    BINARY(MessageKeys.WS_ICON_BINARY),
    SENT(MessageKeys.WS_ICON_SENT),
    CLOSED(MessageKeys.WS_ICON_CLOSED),
    WARNING(MessageKeys.WS_ICON_WARNING),
    INFO(MessageKeys.WS_ICON_INFO);

    final String iconKey;

    WebSocketMsgType(String iconKey) {
        this.iconKey = iconKey;
    }

}
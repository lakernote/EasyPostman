package com.laker.postman.model;

import lombok.Getter;

@Getter
public enum RequestItemProtocolEnum {
    HTTP("HTTP"),
    WEBSOCKET("WebSocket"),
    SSE("SSE"),
    SAVED_RESPONSE("SavedResponse");

    private final String protocol;

    RequestItemProtocolEnum(String protocol) {
        this.protocol = protocol;
    }

    public boolean isWebSocketProtocol() {
        return this == WEBSOCKET;
    }

    public boolean isHttpProtocol() {
        return this == HTTP;
    }

    public boolean isSseProtocol() {
        return this == SSE;
    }
}

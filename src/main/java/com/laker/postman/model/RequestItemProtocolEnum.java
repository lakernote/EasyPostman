package com.laker.postman.model;


public enum RequestItemProtocolEnum {
    HTTP("HTTP"),
    WEBSOCKET("WebSocket"),
    SSE("SSE");

    private final String protocol;

    RequestItemProtocolEnum(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
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
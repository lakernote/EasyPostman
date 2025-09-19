package com.laker.postman.model;


import com.formdev.flatlaf.extras.FlatSVGIcon;
import lombok.Getter;

import javax.swing.*;

@Getter
public enum RequestItemProtocolEnum {
    HTTP("HTTP", new FlatSVGIcon("icons/http.svg", 20, 20)),
    WEBSOCKET("WebSocket", new FlatSVGIcon("icons/websocket.svg", 20, 20)),
    SSE("SSE", new FlatSVGIcon("icons/sse.svg", 20, 20));

    private final String protocol;
    private final Icon icon;

    RequestItemProtocolEnum(String protocol, Icon icon) {
        this.protocol = protocol;
        this.icon = icon;
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
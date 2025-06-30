package com.laker.postman.service.http.okhttp;

import com.laker.postman.panel.SidebarTabPanel;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

// 日志增强WebSocketListener
public class LogWebSocketListener extends WebSocketListener {
    private final WebSocketListener delegate;

    public LogWebSocketListener(WebSocketListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        SidebarTabPanel.appendConsoleLog("[WebSocket] onOpen: " + response);
        delegate.onOpen(webSocket, response);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        SidebarTabPanel.appendConsoleLog("[WebSocket] onMessage: " + text);
        delegate.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
        SidebarTabPanel.appendConsoleLog("[WebSocket] onMessage(bytes): " + bytes.hex());
        delegate.onMessage(webSocket, bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        SidebarTabPanel.appendConsoleLog("[WebSocket] onClosing: code=" + code + ", reason=" + reason);
        delegate.onClosing(webSocket, code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        SidebarTabPanel.appendConsoleLog("[WebSocket] onClosed: code=" + code + ", reason=" + reason);
        delegate.onClosed(webSocket, code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        SidebarTabPanel.appendConsoleLog("[WebSocket] onFailure: " + t.getMessage());
        delegate.onFailure(webSocket, t, response);
    }
}

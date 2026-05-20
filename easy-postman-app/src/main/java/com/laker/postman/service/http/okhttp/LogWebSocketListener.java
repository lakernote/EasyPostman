package com.laker.postman.service.http.okhttp;

import com.laker.postman.panel.sidebar.ConsolePanel;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.awt.GraphicsEnvironment;

// 日志增强WebSocketListener
public class LogWebSocketListener extends WebSocketListener {
    private final WebSocketListener delegate;

    public LogWebSocketListener(WebSocketListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        appendLogSafely("[WebSocket] onOpen: " + response, ConsolePanel.LogType.SUCCESS);
        delegate.onOpen(webSocket, response);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
//        ConsolePanel.appendLog("[WebSocket] onMessage: " + text, ConsolePanel.LogType.INFO);
        delegate.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
//        ConsolePanel.appendLog("[WebSocket] onMessage(bytes): " + bytes.hex(), ConsolePanel.LogType.INFO);
        delegate.onMessage(webSocket, bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        appendLogSafely("[WebSocket] onClosing: code=" + code + ", reason=" + reason, ConsolePanel.LogType.WARN);
        delegate.onClosing(webSocket, code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        appendLogSafely("[WebSocket] onClosed: code=" + code + ", reason=" + reason, ConsolePanel.LogType.DEBUG);
        delegate.onClosed(webSocket, code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        appendLogSafely("[WebSocket] onFailure: " + (t != null ? t.getMessage() : "Unknown error"), ConsolePanel.LogType.ERROR);
        delegate.onFailure(webSocket, t, response);
    }

    private void appendLogSafely(String message, ConsolePanel.LogType type) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            ConsolePanel.appendLog(message, type);
        } catch (RuntimeException ignored) {
            // Logging must not break WebSocket lifecycle callbacks.
        }
    }
}

package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.http.runtime.observation.HttpLifecycleLogSink;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketLifecycleLogListener extends WebSocketListener {
    private final WebSocketListener delegate;
    private final HttpLifecycleLogSink logSink;

    public WebSocketLifecycleLogListener(WebSocketListener delegate) {
        this(delegate, HttpLifecycleLogSink.noop());
    }

    public WebSocketLifecycleLogListener(WebSocketListener delegate, HttpLifecycleLogSink logSink) {
        this.delegate = delegate;
        this.logSink = logSink == null ? HttpLifecycleLogSink.noop() : logSink;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        appendLogSafely("[WebSocket] onOpen: " + response, HttpLifecycleLogSink.Level.SUCCESS);
        delegate.onOpen(webSocket, response);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        delegate.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
        delegate.onMessage(webSocket, bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        appendLogSafely("[WebSocket] onClosing: code=" + code + ", reason=" + reason, HttpLifecycleLogSink.Level.WARN);
        delegate.onClosing(webSocket, code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        appendLogSafely("[WebSocket] onClosed: code=" + code + ", reason=" + reason, HttpLifecycleLogSink.Level.DEBUG);
        delegate.onClosed(webSocket, code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        appendLogSafely("[WebSocket] onFailure: " + (t != null ? t.getMessage() : "Unknown error"), HttpLifecycleLogSink.Level.ERROR);
        delegate.onFailure(webSocket, t, response);
    }

    private void appendLogSafely(String message, HttpLifecycleLogSink.Level level) {
        try {
            logSink.append(message, level);
        } catch (RuntimeException ignored) {
            // Logging must not break WebSocket lifecycle callbacks.
        }
    }
}

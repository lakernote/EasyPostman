package com.laker.postman.service.http.okhttp;

import com.laker.postman.panel.sidebar.ConsolePanel;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

// 日志增强EventSourceListener
public class LogEventSourceListener extends EventSourceListener {
    private final EventSourceListener delegate;

    public LogEventSourceListener(EventSourceListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        ConsolePanel.appendLog("[SSE] onOpen: " + response, ConsolePanel.LogType.SUCCESS);
        delegate.onOpen(eventSource, response);
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        ConsolePanel.appendLog("[SSE] onEvent: id=" + id + ", type=" + type + ", data=" + data, ConsolePanel.LogType.INFO);
        delegate.onEvent(eventSource, id, type, data);
    }

    @Override
    public void onClosed(EventSource eventSource) {
        ConsolePanel.appendLog("[SSE] onClosed", ConsolePanel.LogType.DEBUG);
        delegate.onClosed(eventSource);
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        ConsolePanel.appendLog("[SSE] onFailure: " + (t != null ? t.getMessage() : "Unknown error"), ConsolePanel.LogType.ERROR);
        delegate.onFailure(eventSource, t, response);
    }
}
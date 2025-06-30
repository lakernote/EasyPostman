package com.laker.postman.service.http.okhttp;

import com.laker.postman.panel.SidebarTabPanel;
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
        SidebarTabPanel.appendConsoleLog("[SSE] onOpen: " + response);
        delegate.onOpen(eventSource, response);
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        SidebarTabPanel.appendConsoleLog("[SSE] onEvent: id=" + id + ", type=" + type + ", data=" + data);
        delegate.onEvent(eventSource, id, type, data);
    }

    @Override
    public void onClosed(EventSource eventSource) {
        SidebarTabPanel.appendConsoleLog("[SSE] onClosed");
        delegate.onClosed(eventSource);
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        SidebarTabPanel.appendConsoleLog("[SSE] onFailure: " + t.getMessage());
        delegate.onFailure(eventSource, t, response);
    }
}
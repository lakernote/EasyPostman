package com.laker.postman.panel.performance.execution;


import com.laker.postman.service.http.HttpBaseClientProvider;
import com.laker.postman.service.http.HttpCallTracker;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.Set;

public interface PerformanceNetworkRuntime extends HttpCallTracker, HttpBaseClientProvider {
    Set<EventSource> activeSseSources();

    Set<WebSocket> activeWebSockets();

    int activeHttpCallCount();

    int activeSseCount();

    int activeWebSocketCount();

    void cancelAll();
}

package com.laker.postman.performance.execution;


import com.laker.postman.http.runtime.transport.HttpBaseClientProvider;
import com.laker.postman.http.runtime.transport.HttpCallTracker;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.Set;

public interface PerformanceNetworkRuntime extends HttpCallTracker, HttpBaseClientProvider {
    default void beginRun() {
    }

    Set<EventSource> activeSseSources();

    Set<WebSocket> activeWebSockets();

    int activeHttpCallCount();

    int activeSseCount();

    int activeWebSocketCount();

    void cancelAll();

    default void endRun() {
    }
}

package com.laker.postman.performance.execution;


import com.laker.postman.http.runtime.transport.HttpBaseClientProvider;
import com.laker.postman.http.runtime.transport.HttpCallTracker;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;

import java.util.Set;

public interface PerformanceNetworkRuntime extends HttpCallTracker, HttpBaseClientProvider {
    default void beginRun() {
    }

    Set<RealtimeConnectionHandle> activeSseSources();

    Set<RealtimeWebSocketConnection> activeWebSockets();

    int activeHttpCallCount();

    int activeSseCount();

    int activeWebSocketCount();

    void cancelAll();

    default void endRun() {
    }
}

package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.runtime.PerformanceNetworkControl;
import okhttp3.Call;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class OkHttpActiveRequestRegistry implements PerformanceNetworkControl {
    private final Set<Call> activeHttpCalls = ConcurrentHashMap.newKeySet();
    private final Set<EventSource> activeSseSources = ConcurrentHashMap.newKeySet();
    private final Set<WebSocket> activeWebSockets = ConcurrentHashMap.newKeySet();

    void addHttpCall(Call call) {
        if (call != null) {
            activeHttpCalls.add(call);
        }
    }

    void removeHttpCall(Call call) {
        if (call != null) {
            activeHttpCalls.remove(call);
        }
    }

    void addSseSource(EventSource eventSource) {
        if (eventSource != null) {
            activeSseSources.add(eventSource);
        }
    }

    void removeSseSource(EventSource eventSource) {
        if (eventSource != null) {
            activeSseSources.remove(eventSource);
        }
    }

    void addWebSocket(WebSocket webSocket) {
        if (webSocket != null) {
            activeWebSockets.add(webSocket);
        }
    }

    void removeWebSocket(WebSocket webSocket) {
        if (webSocket != null) {
            activeWebSockets.remove(webSocket);
        }
    }

    @Override
    public int activeHttpCallCount() {
        return activeHttpCalls.size();
    }

    @Override
    public int activeSseCount() {
        return activeSseSources.size();
    }

    @Override
    public int activeWebSocketCount() {
        return activeWebSockets.size();
    }

    @Override
    public void cancelAll() {
        for (Call call : new ArrayList<>(activeHttpCalls)) {
            call.cancel();
        }
        activeHttpCalls.clear();
        for (EventSource eventSource : new ArrayList<>(activeSseSources)) {
            eventSource.cancel();
        }
        activeSseSources.clear();
        for (WebSocket webSocket : new ArrayList<>(activeWebSockets)) {
            webSocket.close(1000, "Performance stopped");
            webSocket.cancel();
        }
        activeWebSockets.clear();
    }
}

package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.runtime.PerformanceNetworkControl;
import okhttp3.Call;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class OkHttpActiveRequestRegistry implements PerformanceNetworkControl {
    private final Set<Call> activeHttpCalls = ConcurrentHashMap.newKeySet();
    private final Set<EventSource> activeSseSources = ConcurrentHashMap.newKeySet();
    private final Set<WebSocket> activeWebSockets = ConcurrentHashMap.newKeySet();
    private volatile boolean cancelling;

    void addHttpCall(Call call) {
        if (call != null) {
            activeHttpCalls.add(call);
            if (cancelling) {
                call.cancel();
            }
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
            if (cancelling) {
                eventSource.cancel();
            }
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
            if (cancelling) {
                cancelWebSocket(webSocket);
            }
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
        cancelling = true;
        try {
            cancelActive(activeHttpCalls, Call::cancel);
            cancelActive(activeSseSources, EventSource::cancel);
            cancelActive(activeWebSockets, OkHttpActiveRequestRegistry::cancelWebSocket);
        } finally {
            cancelling = false;
        }
    }

    private static <T> void cancelActive(Set<T> activeItems, Consumer<T> cancellationAction) {
        for (int pass = 0; pass < 4; pass++) {
            List<T> snapshot = new ArrayList<>(activeItems);
            if (snapshot.isEmpty()) {
                return;
            }
            for (T item : snapshot) {
                cancellationAction.accept(item);
                activeItems.remove(item);
            }
        }
    }

    private static void cancelWebSocket(WebSocket webSocket) {
        webSocket.close(1000, "Performance stopped");
        webSocket.cancel();
    }
}

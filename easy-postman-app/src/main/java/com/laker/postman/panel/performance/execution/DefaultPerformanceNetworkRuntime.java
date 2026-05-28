package com.laker.postman.panel.performance.execution;


import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.ScopedHttpBaseClientProvider;
import com.laker.postman.service.http.okhttp.HttpClientRuntimeConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public final class DefaultPerformanceNetworkRuntime implements PerformanceNetworkRuntime {
    private final Set<Call> activeHttpCalls;
    private final Set<EventSource> activeSseSources;
    private final Set<WebSocket> activeWebSockets;
    private final ScopedHttpBaseClientProvider httpClientProvider;

    public DefaultPerformanceNetworkRuntime() {
        this(HttpClientRuntimeConfig::defaults);
    }

    public DefaultPerformanceNetworkRuntime(Supplier<HttpClientRuntimeConfig> httpClientConfigSupplier) {
        this(
                ConcurrentHashMap.newKeySet(),
                ConcurrentHashMap.newKeySet(),
                ConcurrentHashMap.newKeySet(),
                httpClientConfigSupplier
        );
    }

    DefaultPerformanceNetworkRuntime(Set<EventSource> activeSseSources,
                                     Set<WebSocket> activeWebSockets) {
        this(
                ConcurrentHashMap.newKeySet(),
                activeSseSources,
                activeWebSockets,
                HttpClientRuntimeConfig::defaults
        );
    }

    private DefaultPerformanceNetworkRuntime(Set<Call> activeHttpCalls,
                                             Set<EventSource> activeSseSources,
                                             Set<WebSocket> activeWebSockets,
                                             Supplier<HttpClientRuntimeConfig> httpClientConfigSupplier) {
        this.activeHttpCalls = activeHttpCalls == null ? ConcurrentHashMap.newKeySet() : activeHttpCalls;
        this.activeSseSources = activeSseSources == null ? ConcurrentHashMap.newKeySet() : activeSseSources;
        this.activeWebSockets = activeWebSockets == null ? ConcurrentHashMap.newKeySet() : activeWebSockets;
        this.httpClientProvider = new ScopedHttpBaseClientProvider(httpClientConfigSupplier);
    }

    @Override
    public void onCallStarted(Call call) {
        if (call != null) {
            activeHttpCalls.add(call);
        }
    }

    @Override
    public void onCallFinished(Call call) {
        if (call != null) {
            activeHttpCalls.remove(call);
        }
    }

    @Override
    public Set<EventSource> activeSseSources() {
        return activeSseSources;
    }

    @Override
    public Set<WebSocket> activeWebSockets() {
        return activeWebSockets;
    }

    @Override
    public OkHttpClient getBaseClient(PreparedRequest request) {
        return httpClientProvider.getBaseClient(request);
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
        cancelHttpCalls();
        cancelSseSources();
        cancelWebSockets();
        httpClientProvider.clear();
    }

    private void cancelHttpCalls() {
        int cancelled = 0;
        for (Call call : new ArrayList<>(activeHttpCalls)) {
            try {
                call.cancel();
                cancelled++;
            } catch (Exception e) {
                log.debug("取消压测 HTTP Call 失败", e);
            }
        }
        activeHttpCalls.clear();
        if (cancelled > 0) {
            log.info("取消了 {} 个压测 HTTP Call", cancelled);
        }
    }

    private void cancelSseSources() {
        for (EventSource eventSource : new ArrayList<>(activeSseSources)) {
            try {
                eventSource.cancel();
            } catch (Exception e) {
                log.debug("取消 SSE EventSource 失败", e);
            }
        }
        activeSseSources.clear();
    }

    private void cancelWebSockets() {
        for (WebSocket webSocket : new ArrayList<>(activeWebSockets)) {
            try {
                webSocket.close(1000, "Performance stopped");
            } catch (Exception ignored) {
            }
            try {
                webSocket.cancel();
            } catch (Exception e) {
                log.debug("取消 WebSocket 失败", e);
            }
        }
        activeWebSockets.clear();
    }
}

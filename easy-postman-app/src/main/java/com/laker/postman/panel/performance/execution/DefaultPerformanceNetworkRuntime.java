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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public final class DefaultPerformanceNetworkRuntime implements PerformanceNetworkRuntime {
    private final Set<Call> activeHttpCalls;
    private final Set<EventSource> activeSseSources;
    private final Set<WebSocket> activeWebSockets;
    private final Supplier<HttpClientRuntimeConfig> httpClientConfigSupplier;
    private final ScopedHttpBaseClientProvider httpClientProvider;
    private volatile boolean cancelling;
    private volatile HttpClientRuntimeConfig activeRunConfig;

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
        this.httpClientConfigSupplier = httpClientConfigSupplier == null
                ? HttpClientRuntimeConfig::defaults
                : httpClientConfigSupplier;
        this.httpClientProvider = new ScopedHttpBaseClientProvider(this::currentHttpClientConfig);
    }

    @Override
    public void beginRun() {
        activeRunConfig = resolveConfiguredHttpClientConfig();
        httpClientProvider.clear();
    }

    @Override
    public void onCallStarted(Call call) {
        if (call != null) {
            activeHttpCalls.add(call);
            if (cancelling) {
                call.cancel();
            }
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
        cancelling = true;
        try {
            cancelHttpCalls();
            cancelSseSources();
            cancelWebSockets();
            httpClientProvider.clear();
        } finally {
            cancelling = false;
        }
    }

    @Override
    public void endRun() {
        httpClientProvider.clear();
        activeRunConfig = null;
    }

    private void cancelHttpCalls() {
        int cancelled = cancelActive(activeHttpCalls, call -> {
            try {
                call.cancel();
            } catch (Exception e) {
                log.debug("取消压测 HTTP Call 失败", e);
            }
        });
        if (cancelled > 0) {
            log.info("取消了 {} 个压测 HTTP Call", cancelled);
        }
    }

    private void cancelSseSources() {
        cancelActive(activeSseSources, eventSource -> {
            try {
                eventSource.cancel();
            } catch (Exception e) {
                log.debug("取消 SSE EventSource 失败", e);
            }
        });
    }

    private void cancelWebSockets() {
        cancelActive(activeWebSockets, webSocket -> {
            try {
                webSocket.close(1000, "Performance stopped");
            } catch (Exception ignored) {
            }
            try {
                webSocket.cancel();
            } catch (Exception e) {
                log.debug("取消 WebSocket 失败", e);
            }
        });
    }

    private static <T> int cancelActive(Set<T> activeItems, Consumer<T> cancellationAction) {
        int cancelled = 0;
        for (int pass = 0; pass < 4; pass++) {
            List<T> snapshot = new ArrayList<>(activeItems);
            if (snapshot.isEmpty()) {
                break;
            }
            for (T item : snapshot) {
                cancellationAction.accept(item);
                activeItems.remove(item);
                cancelled++;
            }
        }
        return cancelled;
    }

    private HttpClientRuntimeConfig currentHttpClientConfig() {
        HttpClientRuntimeConfig runConfig = activeRunConfig;
        return runConfig == null ? resolveConfiguredHttpClientConfig() : runConfig;
    }

    private HttpClientRuntimeConfig resolveConfiguredHttpClientConfig() {
        HttpClientRuntimeConfig config = httpClientConfigSupplier.get();
        return config == null ? HttpClientRuntimeConfig.defaults() : config;
    }
}

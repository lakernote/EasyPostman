package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import com.laker.postman.performance.core.runtime.PerformanceTransportRuntime;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

import java.io.IOException;

public final class OkHttpPerformanceTransportRuntime implements PerformanceTransportRuntime, AutoCloseable {
    private final OkHttpActiveRequestRegistry activeRequests;
    private final OkHttpClient baseClient;
    private final boolean ownsBaseClient;
    private final OkHttpHttpSampleExecutor httpExecutor;
    private final OkHttpSseSampleExecutor sseExecutor;
    private final OkHttpWebSocketSampleExecutor webSocketExecutor;

    public OkHttpPerformanceTransportRuntime() {
        this(new OkHttpClient(), true);
    }

    public OkHttpPerformanceTransportRuntime(OkHttpClient baseClient) {
        this(baseClient, false);
    }

    private OkHttpPerformanceTransportRuntime(OkHttpClient baseClient, boolean ownsBaseClient) {
        this.activeRequests = new OkHttpActiveRequestRegistry();
        this.baseClient = baseClient == null ? new OkHttpClient() : baseClient;
        this.ownsBaseClient = ownsBaseClient || baseClient == null;
        OkHttpClientResolver clientResolver = new OkHttpClientResolver(this.baseClient);
        OkHttpRequestFactory requestFactory = new OkHttpRequestFactory();
        this.httpExecutor = new OkHttpHttpSampleExecutor(clientResolver, requestFactory, activeRequests);
        this.sseExecutor = new OkHttpSseSampleExecutor(clientResolver, requestFactory, activeRequests);
        this.webSocketExecutor = new OkHttpWebSocketSampleExecutor(clientResolver, requestFactory, activeRequests);
    }

    @Override
    public PerformanceSampleRecord execute(PerformanceOutboundRequest request) {
        PerformanceOutboundRequest outboundRequest = request == null
                ? PerformanceOutboundRequest.builder().build()
                : request;
        long startTimeMs = System.currentTimeMillis();
        try {
            if (outboundRequest.getProtocol() == PerformanceProtocol.SSE) {
                return sseExecutor.execute(outboundRequest, startTimeMs);
            }
            if (outboundRequest.getProtocol() == PerformanceProtocol.WEBSOCKET) {
                return webSocketExecutor.execute(outboundRequest, startTimeMs);
            }
            return httpExecutor.execute(outboundRequest, startTimeMs);
        } catch (Exception exception) {
            return OkHttpSampleRecords.failedRecord(outboundRequest, startTimeMs, exception);
        }
    }

    @Override
    public int activeHttpCallCount() {
        return activeRequests.activeHttpCallCount();
    }

    @Override
    public int activeSseCount() {
        return activeRequests.activeSseCount();
    }

    @Override
    public int activeWebSocketCount() {
        return activeRequests.activeWebSocketCount();
    }

    @Override
    public void cancelAll() {
        activeRequests.cancelAll();
    }

    @Override
    public void close() {
        cancelAll();
        if (!ownsBaseClient) {
            return;
        }
        baseClient.dispatcher().executorService().shutdown();
        baseClient.connectionPool().evictAll();
        Cache cache = baseClient.cache();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException ignored) {
            }
        }
    }
}

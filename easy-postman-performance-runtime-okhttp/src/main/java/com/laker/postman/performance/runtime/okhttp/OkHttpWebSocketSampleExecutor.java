package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

final class OkHttpWebSocketSampleExecutor {
    private final OkHttpClientResolver clientResolver;
    private final OkHttpRequestFactory requestFactory;
    private final OkHttpActiveRequestRegistry activeRequests;

    OkHttpWebSocketSampleExecutor(OkHttpClientResolver clientResolver,
                                  OkHttpRequestFactory requestFactory,
                                  OkHttpActiveRequestRegistry activeRequests) {
        this.clientResolver = clientResolver;
        this.requestFactory = requestFactory;
        this.activeRequests = activeRequests;
    }

    PerformanceSampleRecord execute(PerformanceOutboundRequest request, long startTimeMs) {
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger responseCode = new AtomicInteger();
        AtomicInteger receivedMessages = new AtomicInteger();
        AtomicLong firstMessageLatencyMs = new AtomicLong(-1);
        AtomicBoolean opened = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        WebSocket webSocket = clientResolver.clientFor(request)
                .newWebSocket(requestFactory.build(request), new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        opened.set(true);
                        responseCode.set(response == null ? 0 : response.code());
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        recordMessage(receivedMessages, firstMessageLatencyMs, startTimeMs);
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, ByteString bytes) {
                        recordMessage(receivedMessages, firstMessageLatencyMs, startTimeMs);
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        webSocket.close(code, reason);
                    }

                    @Override
                    public void onClosed(WebSocket webSocket, int code, String reason) {
                        done.countDown();
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
                        failed.set(true);
                        if (response != null) {
                            responseCode.set(response.code());
                        }
                        errorMessage.set(throwable == null ? "" : throwable.getMessage());
                        done.countDown();
                    }
                });
        activeRequests.addWebSocket(webSocket);
        boolean completed;
        try {
            completed = done.await(waitTimeoutMs(request), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            webSocket.cancel();
            activeRequests.removeWebSocket(webSocket);
            return OkHttpSampleRecords.interruptedRecord(request, startTimeMs, exception);
        }
        if (!completed) {
            webSocket.cancel();
            failed.set(true);
            errorMessage.compareAndSet("", I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_WS_REQUEST_TIMEOUT));
        }
        activeRequests.removeWebSocket(webSocket);
        return OkHttpSampleRecords.streamRecord(
                request,
                PerformanceProtocol.WEBSOCKET,
                startTimeMs,
                responseCode.get(),
                receivedMessages.get(),
                firstMessageLatencyMs.get(),
                errorMessage.get(),
                failed.get(),
                opened.get() && !failed.get()
        );
    }

    private static void recordMessage(AtomicInteger receivedMessages,
                                      AtomicLong firstMessageLatencyMs,
                                      long startTimeMs) {
        if (receivedMessages.getAndIncrement() == 0) {
            firstMessageLatencyMs.compareAndSet(-1, System.currentTimeMillis() - startTimeMs);
        }
    }

    private static long waitTimeoutMs(PerformanceOutboundRequest request) {
        Integer timeoutMs = request.getRequestTimeoutMs();
        if (timeoutMs != null && timeoutMs > 0) {
            return timeoutMs + 1000L;
        }
        return TimeUnit.SECONDS.toMillis(60);
    }
}

package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

final class OkHttpSseSampleExecutor {
    private final OkHttpClientResolver clientResolver;
    private final OkHttpRequestFactory requestFactory;
    private final OkHttpActiveRequestRegistry activeRequests;

    OkHttpSseSampleExecutor(OkHttpClientResolver clientResolver,
                            OkHttpRequestFactory requestFactory,
                            OkHttpActiveRequestRegistry activeRequests) {
        this.clientResolver = clientResolver;
        this.requestFactory = requestFactory;
        this.activeRequests = activeRequests;
    }

    PerformanceSampleRecord execute(PerformanceOutboundRequest request, long startTimeMs) {
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger responseCode = new AtomicInteger();
        AtomicInteger receivedEvents = new AtomicInteger();
        AtomicLong firstEventLatencyMs = new AtomicLong(-1);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        EventSource eventSource = EventSources.createFactory(clientResolver.clientFor(request))
                .newEventSource(requestFactory.build(request), new EventSourceListener() {
                    @Override
                    public void onOpen(EventSource eventSource, Response response) {
                        responseCode.set(response == null ? 0 : response.code());
                    }

                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        if (receivedEvents.getAndIncrement() == 0) {
                            firstEventLatencyMs.compareAndSet(-1, System.currentTimeMillis() - startTimeMs);
                        }
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        done.countDown();
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable throwable, Response response) {
                        failed.set(true);
                        if (response != null) {
                            responseCode.set(response.code());
                        }
                        errorMessage.set(throwable == null ? "" : throwable.getMessage());
                        done.countDown();
                    }
                });
        activeRequests.addSseSource(eventSource);
        boolean completed;
        try {
            completed = done.await(waitTimeoutMs(request), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            eventSource.cancel();
            activeRequests.removeSseSource(eventSource);
            return OkHttpSampleRecords.interruptedRecord(request, startTimeMs, exception);
        }
        if (!completed) {
            eventSource.cancel();
            failed.set(true);
            errorMessage.compareAndSet("", "SSE request timed out");
        }
        activeRequests.removeSseSource(eventSource);
        boolean successful = !failed.get() && responseCode.get() >= 200 && responseCode.get() < 400;
        return OkHttpSampleRecords.streamRecord(
                request,
                PerformanceProtocol.SSE,
                startTimeMs,
                responseCode.get(),
                receivedEvents.get(),
                firstEventLatencyMs.get(),
                errorMessage.get(),
                failed.get(),
                successful
        );
    }

    private static long waitTimeoutMs(PerformanceOutboundRequest request) {
        Integer timeoutMs = request.getRequestTimeoutMs();
        if (timeoutMs != null && timeoutMs > 0) {
            return timeoutMs + 1000L;
        }
        return TimeUnit.SECONDS.toMillis(60);
    }
}

package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import com.laker.postman.service.http.HttpSingleRequestExecutor;

public class SseSampleExecutor {

    public static final class Result {
        public final HttpResponse response;
        public final String errorMsg;
        public final boolean executionFailed;
        public final boolean interrupted;

        public Result(HttpResponse response, String errorMsg, boolean executionFailed, boolean interrupted) {
            this.response = response;
            this.errorMsg = errorMsg;
            this.executionFailed = executionFailed;
            this.interrupted = interrupted;
        }
    }

    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<EventSource> activeSources;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final int responseBodyPreviewLimitBytes;

    public SseSampleExecutor(BooleanSupplier runningSupplier,
                             Predicate<Throwable> cancelledChecker,
                             Set<EventSource> activeSources) {
        this(runningSupplier, cancelledChecker, activeSources, new PerformanceRealtimeMetrics());
    }

    public SseSampleExecutor(BooleanSupplier runningSupplier,
                             Predicate<Throwable> cancelledChecker,
                             Set<EventSource> activeSources,
                             PerformanceRealtimeMetrics realtimeMetrics) {
        this(runningSupplier, cancelledChecker, activeSources, realtimeMetrics, BoundedTextAccumulator.DEFAULT_PREVIEW_BYTES);
    }

    public SseSampleExecutor(BooleanSupplier runningSupplier,
                             Predicate<Throwable> cancelledChecker,
                             Set<EventSource> activeSources,
                             PerformanceRealtimeMetrics realtimeMetrics,
                             int responseBodyPreviewLimitBytes) {
        this.runningSupplier = runningSupplier;
        this.cancelledChecker = cancelledChecker;
        this.activeSources = activeSources;
        this.realtimeMetrics = realtimeMetrics == null ? new PerformanceRealtimeMetrics() : realtimeMetrics;
        this.responseBodyPreviewLimitBytes = Math.max(1, responseBodyPreviewLimitBytes);
    }

    public Result execute(PreparedRequest req, SsePerformanceData cfg) {
        return execute(req, cfg, "", "");
    }

    public Result execute(PreparedRequest req, SsePerformanceData cfg, String apiId, String apiName) {
        long requestStartTime = System.currentTimeMillis();
        HttpResponse resp = new HttpResponse();
        resp.isSse = true;

        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean closingSource = new AtomicBoolean(false);
        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        AtomicReference<String> errorRef = new AtomicReference<>("");
        AtomicReference<String> lastEventIdRef = new AtomicReference<>("");
        AtomicReference<String> lastEventTypeRef = new AtomicReference<>("");
        BoundedTextAccumulator matchedEventBody = new BoundedTextAccumulator(responseBodyPreviewLimitBytes);
        AtomicLong sampleEndTimeMs = new AtomicLong(0);
        AtomicLong firstEventLatencyMs = new AtomicLong(-1);
        AtomicBoolean firstEventRecorded = new AtomicBoolean(false);
        AtomicBoolean sessionRegistered = new AtomicBoolean(false);
        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicInteger matchedMessageCount = new AtomicInteger(0);
        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch firstMessageLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(1);

        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                resp.headers = new LinkedHashMap<>();
                for (String name : response.headers().names()) {
                    resp.addHeader(name, response.headers(name));
                }
                resp.code = response.code();
                resp.protocol = response.protocol().toString();
                resp.isSse = true;
                if (sessionRegistered.compareAndSet(false, true)) {
                    realtimeMetrics.recordSseSessionStart(eventSource, requestStartTime, apiId, apiName);
                }
                openLatch.countDown();
            }

            @Override
            public void onClosed(EventSource eventSource) {
                connectionClosed.set(true);
                openLatch.countDown();
                firstMessageLatch.countDown();
                completionLatch.countDown();
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable throwable, Response response) {
                if (response != null) {
                    if (resp.headers == null) {
                        resp.headers = new LinkedHashMap<>();
                    }
                    for (String name : response.headers().names()) {
                        resp.addHeader(name, response.headers(name));
                    }
                    resp.code = response.code();
                    resp.protocol = response.protocol().toString();
                }
                String message = throwable != null ? throwable.getMessage() : "";
                if (!closingSource.get() && !failed.get()) {
                    if (!runningSupplier.getAsBoolean() || cancelledChecker.test(throwable)) {
                        interrupted.set(true);
                    } else {
                        failed.set(true);
                        errorRef.set(CharSequenceUtil.blankToDefault(message, "SSE request failed"));
                    }
                }
                openLatch.countDown();
                firstMessageLatch.countDown();
                completionLatch.countDown();
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                eventCount.incrementAndGet();
                realtimeMetrics.recordSseReceived(eventSource);
                String eventType = CharSequenceUtil.blankToDefault(type, "message");
                long eventReceivedAtMs = System.currentTimeMillis();
                boolean firstPhysicalEvent = firstEventRecorded.compareAndSet(false, true);
                if (firstPhysicalEvent) {
                    long latencyMs = Math.max(0, eventReceivedAtMs - requestStartTime);
                    firstEventLatencyMs.compareAndSet(-1, latencyMs);
                    realtimeMetrics.recordSseFirstMessageLatency(eventSource, latencyMs);
                }

                if (cfg.completionMode == SsePerformanceData.CompletionMode.FIRST_MESSAGE) {
                    if (firstPhysicalEvent) {
                        matchedMessageCount.incrementAndGet();
                        realtimeMetrics.recordSseMatched(eventSource);
                        lastEventIdRef.set(id == null ? "" : id);
                        lastEventTypeRef.set(eventType);
                        SseEventFormatter.appendEvent(matchedEventBody, id, eventType, data);
                        firstMessageLatch.countDown();
                        completionLatch.countDown();
                    }
                    return;
                }

                if (SseSampleMatcher.matchesEvent(cfg, eventType) && SseSampleMatcher.matchesPayload(cfg, data)) {
                    boolean firstMatchedMessage = matchedMessageCount.incrementAndGet() == 1;
                    realtimeMetrics.recordSseMatched(eventSource);
                    lastEventIdRef.set(id == null ? "" : id);
                    lastEventTypeRef.set(eventType);
                    SseEventFormatter.appendEvent(matchedEventBody, id, eventType, data);
                    if (firstMatchedMessage) {
                        firstMessageLatch.countDown();
                    }

                    if (cfg.completionMode == SsePerformanceData.CompletionMode.FIRST_MESSAGE
                            || cfg.completionMode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE) {
                        completionLatch.countDown();
                    } else if (cfg.completionMode == SsePerformanceData.CompletionMode.MESSAGE_COUNT
                            && matchedMessageCount.get() >= Math.max(1, cfg.targetMessageCount)) {
                        completionLatch.countDown();
                    }
                }
            }
        };

        EventSource eventSource = HttpSingleRequestExecutor.executeSSE(req, listener);
        activeSources.add(eventSource);
        if (sessionRegistered.compareAndSet(false, true)) {
            realtimeMetrics.recordSseSessionStart(eventSource, requestStartTime, apiId, apiName);
        }

        try {
            switch (cfg.completionMode) {
                case FIRST_MESSAGE -> {
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        closingSource.set(true);
                        markSampleEnd(sampleEndTimeMs);
                        eventSource.cancel();
                    }
                    boolean gotFirstMessage = !failed.get() && !interrupted.get()
                            && firstMessageLatch.await(Math.max(100, cfg.firstMessageTimeoutMs), TimeUnit.MILLISECONDS);
                    if ((!gotFirstMessage || matchedMessageCount.get() == 0) && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE first event timeout");
                        closingSource.set(true);
                        markSampleEnd(sampleEndTimeMs);
                        eventSource.cancel();
                    }
                }
                case MATCHED_MESSAGE -> {
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        closingSource.set(true);
                        markSampleEnd(sampleEndTimeMs);
                        eventSource.cancel();
                    }
                    boolean gotMatchedMessage = !failed.get() && !interrupted.get()
                            && firstMessageLatch.await(Math.max(100, cfg.firstMessageTimeoutMs), TimeUnit.MILLISECONDS);
                    if ((!gotMatchedMessage || matchedMessageCount.get() == 0) && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE matched message timeout");
                        closingSource.set(true);
                        markSampleEnd(sampleEndTimeMs);
                        eventSource.cancel();
                    }
                }
                case FIXED_DURATION -> {
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        closingSource.set(true);
                        markSampleEnd(sampleEndTimeMs);
                        eventSource.cancel();
                    } else if (!failed.get() && !interrupted.get()) {
                        boolean terminated = completionLatch.await(Math.max(100, cfg.holdConnectionMs), TimeUnit.MILLISECONDS);
                        if (terminated && !failed.get() && !interrupted.get()) {
                            failed.set(true);
                            errorRef.set("SSE connection closed before hold duration finished");
                        }
                    }
                }
                case MESSAGE_COUNT -> {
                    int targetMessageCount = Math.max(1, cfg.targetMessageCount);
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        closingSource.set(true);
                        markSampleEnd(sampleEndTimeMs);
                        eventSource.cancel();
                    }
                    boolean gotFirstMessage = !failed.get() && !interrupted.get()
                            && firstMessageLatch.await(Math.max(100, cfg.firstMessageTimeoutMs), TimeUnit.MILLISECONDS);
                    if ((!gotFirstMessage || matchedMessageCount.get() == 0) && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE first message timeout");
                        closingSource.set(true);
                        markSampleEnd(sampleEndTimeMs);
                        eventSource.cancel();
                    } else if (!failed.get() && !interrupted.get()
                            && matchedMessageCount.get() < targetMessageCount) {
                        boolean reachedTarget = completionLatch.await(Math.max(100, cfg.holdConnectionMs), TimeUnit.MILLISECONDS);
                        if (matchedMessageCount.get() < targetMessageCount && !failed.get() && !interrupted.get()) {
                            failed.set(true);
                            errorRef.set(reachedTarget && connectionClosed.get()
                                    ? "SSE connection closed before target message count reached"
                                    : "SSE target message count timeout");
                            closingSource.set(true);
                            markSampleEnd(sampleEndTimeMs);
                            eventSource.cancel();
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            interrupted.set(true);
        } finally {
            markSampleEnd(sampleEndTimeMs);
            realtimeMetrics.recordSseSessionEnd(eventSource);
            closingSource.set(true);
            eventSource.cancel();
            activeSources.remove(eventSource);
        }

        long endTime = sampleEndTimeMs.get();
        resp.endTime = endTime;
        resp.costMs = endTime - requestStartTime;
        resp.body = matchedEventBody.value();
        resp.bodySize = matchedEventBody.totalUtf8Bytes();
        SseSampleResponseBuilder.addSummaryHeaders(
                resp,
                cfg,
                eventCount.get(),
                matchedMessageCount.get(),
                firstEventLatencyMs.get(),
                lastEventIdRef.get(),
                lastEventTypeRef.get(),
                errorRef.get()
        );

        return new Result(resp, errorRef.get(), failed.get(), interrupted.get());
    }

    private static void markSampleEnd(AtomicLong sampleEndTimeMs) {
        sampleEndTimeMs.compareAndSet(0, System.currentTimeMillis());
    }

}

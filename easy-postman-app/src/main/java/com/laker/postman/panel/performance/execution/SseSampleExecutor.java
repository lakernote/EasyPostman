package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.Collections;
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
        long requestStartTime = System.currentTimeMillis();
        HttpResponse resp = new HttpResponse();
        resp.isSse = true;

        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean closingSource = new AtomicBoolean(false);
        AtomicReference<String> errorRef = new AtomicReference<>("");
        AtomicReference<String> completionReasonRef = new AtomicReference<>("pending");
        AtomicReference<String> lastEventIdRef = new AtomicReference<>("");
        AtomicReference<String> lastEventTypeRef = new AtomicReference<>("");
        BoundedTextAccumulator matchedEventBody = new BoundedTextAccumulator(responseBodyPreviewLimitBytes);
        AtomicLong firstMessageLatencyMs = new AtomicLong(-1);
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
                openLatch.countDown();
            }

            @Override
            public void onClosed(EventSource eventSource) {
                completionReasonRef.compareAndSet("pending", "closed");
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
                if (closingSource.get()) {
                    completionReasonRef.compareAndSet("pending", "closed");
                } else if (failed.get()) {
                    completionReasonRef.compareAndSet("pending", "failure");
                } else if (!runningSupplier.getAsBoolean() || cancelledChecker.test(throwable)) {
                    interrupted.set(true);
                    completionReasonRef.set("interrupted");
                } else {
                    failed.set(true);
                    errorRef.set(CharSequenceUtil.blankToDefault(message, "SSE request failed"));
                    completionReasonRef.set("failure");
                }
                openLatch.countDown();
                firstMessageLatch.countDown();
                completionLatch.countDown();
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                eventCount.incrementAndGet();
                realtimeMetrics.recordSseReceived();
                String eventType = CharSequenceUtil.blankToDefault(type, "message");
                if (matchesEvent(cfg, eventType) && matchesPayload(cfg, data)) {
                    boolean firstMatchedMessage = matchedMessageCount.incrementAndGet() == 1;
                    realtimeMetrics.recordSseMatched();
                    if (firstMatchedMessage) {
                        long latencyMs = Math.max(0, System.currentTimeMillis() - requestStartTime);
                        firstMessageLatencyMs.compareAndSet(-1, latencyMs);
                        realtimeMetrics.recordSseFirstMessageLatency(latencyMs);
                        completionReasonRef.compareAndSet("pending",
                                cfg.completionMode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE
                                        ? "matched_message"
                                        : "first_message");
                    }
                    lastEventIdRef.set(id == null ? "" : id);
                    lastEventTypeRef.set(eventType);
                    appendEvent(matchedEventBody, id, eventType, data);
                    if (firstMatchedMessage) {
                        firstMessageLatch.countDown();
                    }

                    if (cfg.completionMode == SsePerformanceData.CompletionMode.FIRST_MESSAGE
                            || cfg.completionMode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE) {
                        completionLatch.countDown();
                    } else if (cfg.completionMode == SsePerformanceData.CompletionMode.MESSAGE_COUNT
                            && matchedMessageCount.get() >= Math.max(1, cfg.targetMessageCount)) {
                        completionReasonRef.set("message_target");
                        completionLatch.countDown();
                    }
                }
            }
        };

        EventSource eventSource = HttpSingleRequestExecutor.executeSSE(req, listener);
        activeSources.add(eventSource);
        realtimeMetrics.recordSseSessionStart(eventSource, requestStartTime);

        try {
            switch (cfg.completionMode) {
                case FIRST_MESSAGE -> {
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        completionReasonRef.set("connect_timeout");
                        closingSource.set(true);
                        eventSource.cancel();
                    }
                    boolean gotFirstMessage = !failed.get() && !interrupted.get()
                            && firstMessageLatch.await(Math.max(100, cfg.firstMessageTimeoutMs), TimeUnit.MILLISECONDS);
                    if ((!gotFirstMessage || matchedMessageCount.get() == 0) && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE first message timeout");
                        completionReasonRef.compareAndSet("pending", "first_message_timeout");
                        closingSource.set(true);
                        eventSource.cancel();
                    }
                }
                case MATCHED_MESSAGE -> {
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        completionReasonRef.set("connect_timeout");
                        closingSource.set(true);
                        eventSource.cancel();
                    }
                    boolean gotMatchedMessage = !failed.get() && !interrupted.get()
                            && firstMessageLatch.await(Math.max(100, cfg.firstMessageTimeoutMs), TimeUnit.MILLISECONDS);
                    if ((!gotMatchedMessage || matchedMessageCount.get() == 0) && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE matched message timeout");
                        completionReasonRef.compareAndSet("pending", "matched_message_timeout");
                        closingSource.set(true);
                        eventSource.cancel();
                    }
                }
                case FIXED_DURATION -> {
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        completionReasonRef.set("connect_timeout");
                        closingSource.set(true);
                        eventSource.cancel();
                    } else if (!failed.get() && !interrupted.get()) {
                        boolean terminated = completionLatch.await(Math.max(100, cfg.holdConnectionMs), TimeUnit.MILLISECONDS);
                        if (terminated && !failed.get() && !interrupted.get()) {
                            failed.set(true);
                            errorRef.set("SSE connection closed before hold duration finished");
                            completionReasonRef.set("closed_early");
                        } else if (!terminated) {
                            completionReasonRef.set("hold_complete");
                        }
                    }
                }
                case MESSAGE_COUNT -> {
                    boolean opened = openLatch.await(Math.max(100, cfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                    if (!opened && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE connection timeout");
                        completionReasonRef.set("connect_timeout");
                        closingSource.set(true);
                        eventSource.cancel();
                    }
                    boolean gotFirstMessage = !failed.get() && !interrupted.get()
                            && firstMessageLatch.await(Math.max(100, cfg.firstMessageTimeoutMs), TimeUnit.MILLISECONDS);
                    if ((!gotFirstMessage || matchedMessageCount.get() == 0) && !failed.get() && !interrupted.get()) {
                        failed.set(true);
                        errorRef.set("SSE first message timeout");
                        completionReasonRef.compareAndSet("pending", "first_message_timeout");
                        closingSource.set(true);
                        eventSource.cancel();
                    } else if (!failed.get() && !interrupted.get()
                            && matchedMessageCount.get() < Math.max(1, cfg.targetMessageCount)) {
                        boolean reachedTarget = completionLatch.await(Math.max(100, cfg.holdConnectionMs), TimeUnit.MILLISECONDS);
                        if (!reachedTarget && matchedMessageCount.get() < Math.max(1, cfg.targetMessageCount)
                                && !failed.get() && !interrupted.get()) {
                            failed.set(true);
                            errorRef.set("SSE target message count timeout");
                            completionReasonRef.set("message_target_timeout");
                            closingSource.set(true);
                            eventSource.cancel();
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            interrupted.set(true);
            completionReasonRef.set("interrupted");
        } finally {
            closingSource.set(true);
            eventSource.cancel();
            activeSources.remove(eventSource);
            realtimeMetrics.recordSseSessionEnd(eventSource);
        }

        long endTime = System.currentTimeMillis();
        resp.endTime = endTime;
        resp.costMs = endTime - requestStartTime;
        resp.body = matchedEventBody.value();
        resp.bodySize = matchedEventBody.totalUtf8Bytes();
        if (resp.headers == null) {
            resp.headers = new LinkedHashMap<>();
        }
        resp.addHeader("X-Easy-SSE-Mode", Collections.singletonList(cfg.completionMode.name()));
        resp.addHeader("X-Easy-SSE-Event-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(cfg.eventNameFilter, "")));
        resp.addHeader("X-Easy-SSE-Message-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(cfg.messageFilter, "")));
        resp.addHeader("X-Easy-SSE-Event-Count", Collections.singletonList(String.valueOf(eventCount.get())));
        resp.addHeader("X-Easy-SSE-Message-Count", Collections.singletonList(String.valueOf(matchedMessageCount.get())));
        resp.addHeader("X-Easy-SSE-First-Message-Latency-Ms", Collections.singletonList(firstMessageLatencyMs.get() >= 0 ? String.valueOf(firstMessageLatencyMs.get()) : ""));
        resp.addHeader("X-Easy-SSE-Completion-Reason", Collections.singletonList(CharSequenceUtil.blankToDefault(completionReasonRef.get(), "")));
        resp.addHeader("X-Easy-SSE-Event-Id", Collections.singletonList(CharSequenceUtil.blankToDefault(lastEventIdRef.get(), "")));
        resp.addHeader("X-Easy-SSE-Event-Type", Collections.singletonList(CharSequenceUtil.blankToDefault(lastEventTypeRef.get(), "")));
        if (CharSequenceUtil.isNotBlank(errorRef.get())) {
            resp.addHeader("X-Easy-SSE-Error", Collections.singletonList(errorRef.get()));
        }

        return new Result(resp, errorRef.get(), failed.get(), interrupted.get());
    }

    private boolean matchesEvent(SsePerformanceData cfg, String eventType) {
        if (!SsePerformanceData.usesEventNameFilter(cfg.completionMode)) {
            return true;
        }
        String filter = cfg.eventNameFilter;
        return CharSequenceUtil.isBlank(filter) || CharSequenceUtil.equals(filter.trim(), eventType);
    }

    private boolean matchesPayload(SsePerformanceData cfg, String data) {
        if (cfg.completionMode != SsePerformanceData.CompletionMode.MATCHED_MESSAGE) {
            return true;
        }
        String filter = cfg.messageFilter;
        return CharSequenceUtil.isBlank(filter) || (data != null && data.contains(filter.trim()));
    }

    private void appendEvent(BoundedTextAccumulator buffer, String id, String type, String data) {
        if (id != null && !id.isBlank()) {
            buffer.append("id: ");
            buffer.append(id);
            buffer.append("\n");
        }
        if (type != null && !type.isBlank()) {
            buffer.append("event: ");
            buffer.append(type);
            buffer.append("\n");
        }
        String eventData = data == null ? "" : data;
        appendDataLines(buffer, eventData);
        buffer.append("\n");
    }

    private void appendDataLines(BoundedTextAccumulator buffer, String eventData) {
        int lineStart = 0;
        for (int i = 0; i < eventData.length(); i++) {
            char ch = eventData.charAt(i);
            if (ch != '\n' && ch != '\r') {
                continue;
            }
            buffer.append("data: ");
            buffer.append(eventData, lineStart, i);
            buffer.append("\n");
            if (ch == '\r' && i + 1 < eventData.length() && eventData.charAt(i + 1) == '\n') {
                i++;
            }
            lineStart = i + 1;
        }
        buffer.append("data: ");
        buffer.append(eventData, lineStart, eventData.length());
        buffer.append("\n");
    }
}

package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.nio.charset.StandardCharsets;
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

    public SseSampleExecutor(BooleanSupplier runningSupplier,
                             Predicate<Throwable> cancelledChecker,
                             Set<EventSource> activeSources) {
        this.runningSupplier = runningSupplier;
        this.cancelledChecker = cancelledChecker;
        this.activeSources = activeSources;
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
        StringBuffer matchedEventBody = new StringBuffer();
        AtomicLong firstMessageLatencyMs = new AtomicLong(-1);
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
                String eventType = CharSequenceUtil.blankToDefault(type, "message");
                if (matchesEvent(cfg, eventType)) {
                    if (matchedMessageCount.incrementAndGet() == 1) {
                        firstMessageLatencyMs.compareAndSet(-1, System.currentTimeMillis() - requestStartTime);
                        completionReasonRef.compareAndSet("pending", "first_message");
                        firstMessageLatch.countDown();
                    }
                    lastEventIdRef.set(id == null ? "" : id);
                    lastEventTypeRef.set(eventType);
                    appendEvent(matchedEventBody, id, eventType, data);

                    if (cfg.completionMode == SsePerformanceData.CompletionMode.FIRST_MESSAGE) {
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
        }

        long endTime = System.currentTimeMillis();
        resp.endTime = endTime;
        resp.costMs = endTime - requestStartTime;
        resp.body = matchedEventBody.toString();
        resp.bodySize = resp.body.getBytes(StandardCharsets.UTF_8).length;
        if (resp.headers == null) {
            resp.headers = new LinkedHashMap<>();
        }
        resp.addHeader("X-Easy-SSE-Mode", Collections.singletonList(cfg.completionMode.name()));
        resp.addHeader("X-Easy-SSE-Event-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(cfg.eventNameFilter, "")));
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
        String filter = cfg.eventNameFilter;
        return CharSequenceUtil.isBlank(filter) || CharSequenceUtil.equals(filter.trim(), eventType);
    }

    private void appendEvent(StringBuffer buffer, String id, String type, String data) {
        if (id != null && !id.isBlank()) {
            buffer.append("id: ").append(id).append('\n');
        }
        if (type != null && !type.isBlank()) {
            buffer.append("event: ").append(type).append('\n');
        }
        String eventData = data == null ? "" : data;
        for (String line : eventData.split("\\R", -1)) {
            buffer.append("data: ").append(line).append('\n');
        }
        buffer.append('\n');
    }
}

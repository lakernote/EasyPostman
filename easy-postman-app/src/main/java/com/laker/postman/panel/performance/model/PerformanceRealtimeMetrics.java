package com.laker.postman.panel.performance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceRealtimeMetrics {

    private final AtomicLong webSocketSentMessages = new AtomicLong();
    private final AtomicLong webSocketReceivedMessages = new AtomicLong();
    private final AtomicLong webSocketMatchedMessages = new AtomicLong();
    private final AtomicLong webSocketFirstMessageLatencyTotalMs = new AtomicLong();
    private final AtomicLong webSocketFirstMessageLatencyCount = new AtomicLong();
    private final AtomicLong sseReceivedMessages = new AtomicLong();
    private final AtomicLong sseMatchedMessages = new AtomicLong();
    private final AtomicLong sseFirstMessageLatencyTotalMs = new AtomicLong();
    private final AtomicLong sseFirstMessageLatencyCount = new AtomicLong();
    private final Map<Object, Long> webSocketSessionStarts = new ConcurrentHashMap<>();
    private final Map<Object, Long> sseSessionStarts = new ConcurrentHashMap<>();

    private final AtomicLong lastSampleTimeMs = new AtomicLong();
    private final AtomicLong lastWebSocketSentMessages = new AtomicLong();
    private final AtomicLong lastWebSocketReceivedMessages = new AtomicLong();
    private final AtomicLong lastWebSocketMatchedMessages = new AtomicLong();
    private final AtomicLong lastWebSocketFirstMessageLatencyTotalMs = new AtomicLong();
    private final AtomicLong lastWebSocketFirstMessageLatencyCount = new AtomicLong();
    private final AtomicLong lastSseReceivedMessages = new AtomicLong();
    private final AtomicLong lastSseMatchedMessages = new AtomicLong();
    private final AtomicLong lastSseFirstMessageLatencyTotalMs = new AtomicLong();
    private final AtomicLong lastSseFirstMessageLatencyCount = new AtomicLong();

    public synchronized void reset(long nowMs) {
        webSocketSentMessages.set(0);
        webSocketReceivedMessages.set(0);
        webSocketMatchedMessages.set(0);
        webSocketFirstMessageLatencyTotalMs.set(0);
        webSocketFirstMessageLatencyCount.set(0);
        sseReceivedMessages.set(0);
        sseMatchedMessages.set(0);
        sseFirstMessageLatencyTotalMs.set(0);
        sseFirstMessageLatencyCount.set(0);
        webSocketSessionStarts.clear();
        sseSessionStarts.clear();

        lastSampleTimeMs.set(nowMs);
        lastWebSocketSentMessages.set(0);
        lastWebSocketReceivedMessages.set(0);
        lastWebSocketMatchedMessages.set(0);
        lastWebSocketFirstMessageLatencyTotalMs.set(0);
        lastWebSocketFirstMessageLatencyCount.set(0);
        lastSseReceivedMessages.set(0);
        lastSseMatchedMessages.set(0);
        lastSseFirstMessageLatencyTotalMs.set(0);
        lastSseFirstMessageLatencyCount.set(0);
    }

    public void recordWebSocketSent() {
        webSocketSentMessages.incrementAndGet();
    }

    public void recordWebSocketReceived() {
        webSocketReceivedMessages.incrementAndGet();
    }

    public void recordWebSocketMatched() {
        webSocketMatchedMessages.incrementAndGet();
    }

    public synchronized void recordWebSocketFirstMessageLatency(long latencyMs) {
        webSocketFirstMessageLatencyTotalMs.addAndGet(Math.max(0, latencyMs));
        webSocketFirstMessageLatencyCount.incrementAndGet();
    }

    public void recordWebSocketSessionStart(Object session, long startTimeMs) {
        if (session != null) {
            webSocketSessionStarts.put(session, startTimeMs);
        }
    }

    public void recordWebSocketSessionEnd(Object session) {
        if (session != null) {
            webSocketSessionStarts.remove(session);
        }
    }

    public void recordSseReceived() {
        sseReceivedMessages.incrementAndGet();
    }

    public void recordSseMatched() {
        sseMatchedMessages.incrementAndGet();
    }

    public synchronized void recordSseFirstMessageLatency(long latencyMs) {
        sseFirstMessageLatencyTotalMs.addAndGet(Math.max(0, latencyMs));
        sseFirstMessageLatencyCount.incrementAndGet();
    }

    public void recordSseSessionStart(Object session, long startTimeMs) {
        if (session != null) {
            sseSessionStarts.put(session, startTimeMs);
        }
    }

    public void recordSseSessionEnd(Object session) {
        if (session != null) {
            sseSessionStarts.remove(session);
        }
    }

    public synchronized Sample sample(long nowMs) {
        long previousSampleTimeMs = lastSampleTimeMs.getAndSet(nowMs);
        long elapsedMs = Math.max(1, nowMs - previousSampleTimeMs);
        double seconds = elapsedMs / 1000.0;

        long currentWebSocketSent = webSocketSentMessages.get();
        long currentWebSocketReceived = webSocketReceivedMessages.get();
        long currentWebSocketMatched = webSocketMatchedMessages.get();
        long currentWebSocketLatencyTotal = webSocketFirstMessageLatencyTotalMs.get();
        long currentWebSocketLatencyCount = webSocketFirstMessageLatencyCount.get();
        long currentSseReceived = sseReceivedMessages.get();
        long currentSseMatched = sseMatchedMessages.get();
        long currentSseLatencyTotal = sseFirstMessageLatencyTotalMs.get();
        long currentSseLatencyCount = sseFirstMessageLatencyCount.get();

        long webSocketLatencyCountDelta = currentWebSocketLatencyCount
                - lastWebSocketFirstMessageLatencyCount.getAndSet(currentWebSocketLatencyCount);
        long sseLatencyCountDelta = currentSseLatencyCount
                - lastSseFirstMessageLatencyCount.getAndSet(currentSseLatencyCount);

        return new Sample(
                rate(currentWebSocketSent - lastWebSocketSentMessages.getAndSet(currentWebSocketSent), seconds),
                rate(currentWebSocketReceived - lastWebSocketReceivedMessages.getAndSet(currentWebSocketReceived), seconds),
                rate(currentWebSocketMatched - lastWebSocketMatchedMessages.getAndSet(currentWebSocketMatched), seconds),
                average(
                        currentWebSocketLatencyTotal - lastWebSocketFirstMessageLatencyTotalMs.getAndSet(currentWebSocketLatencyTotal),
                        webSocketLatencyCountDelta
                ),
                activeDuration(webSocketSessionStarts, nowMs),
                rate(currentSseReceived - lastSseReceivedMessages.getAndSet(currentSseReceived), seconds),
                rate(currentSseMatched - lastSseMatchedMessages.getAndSet(currentSseMatched), seconds),
                average(
                        currentSseLatencyTotal - lastSseFirstMessageLatencyTotalMs.getAndSet(currentSseLatencyTotal),
                        sseLatencyCountDelta
                ),
                activeDuration(sseSessionStarts, nowMs)
        );
    }

    private static double rate(long count, double seconds) {
        return seconds > 0 ? round(Math.max(0, count) / seconds) : 0;
    }

    private static double average(long total, long count) {
        return count > 0 ? round((double) Math.max(0, total) / count) : Double.NaN;
    }

    private static double activeDuration(Map<Object, Long> sessionStarts, long nowMs) {
        if (sessionStarts.isEmpty()) {
            return 0;
        }
        long total = 0;
        int count = 0;
        for (Long startTimeMs : sessionStarts.values()) {
            if (startTimeMs == null) {
                continue;
            }
            total += Math.max(0, nowMs - startTimeMs);
            count++;
        }
        return count > 0 ? round((double) total / count) : 0;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record Sample(
            double webSocketSentRate,
            double webSocketReceivedRate,
            double webSocketMatchedRate,
            double webSocketFirstMessageLatencyMs,
            double webSocketActiveSessionDurationMs,
            double sseReceivedRate,
            double sseMatchedRate,
            double sseFirstMessageLatencyMs,
            double sseActiveSessionDurationMs
    ) {
        public static Sample empty() {
            return new Sample(0, 0, 0, Double.NaN, 0, 0, 0, Double.NaN, 0);
        }
    }
}

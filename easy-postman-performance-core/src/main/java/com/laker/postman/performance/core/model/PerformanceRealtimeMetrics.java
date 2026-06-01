package com.laker.postman.performance.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceRealtimeMetrics {

    private static final long MIN_RATE_SAMPLE_INTERVAL_MS = 1L;

    private final AtomicLong webSocketSentMessages = new AtomicLong();
    private final AtomicLong webSocketReceivedMessages = new AtomicLong();
    private final AtomicLong webSocketMatchedMessages = new AtomicLong();
    private final AtomicLong webSocketFirstMessageLatencyTotalMs = new AtomicLong();
    private final AtomicLong webSocketFirstMessageLatencyCount = new AtomicLong();
    private final AtomicLong sseReceivedMessages = new AtomicLong();
    private final AtomicLong sseMatchedMessages = new AtomicLong();
    private final AtomicLong sseFirstMessageLatencyTotalMs = new AtomicLong();
    private final AtomicLong sseFirstMessageLatencyCount = new AtomicLong();
    private final Map<Object, StreamSessionMetrics> webSocketSessionStarts = new ConcurrentHashMap<>();
    private final Map<Object, StreamSessionMetrics> sseSessionStarts = new ConcurrentHashMap<>();
    private final AtomicInteger activeWebSocketSessions = new AtomicInteger();
    private final AtomicInteger peakWebSocketSessions = new AtomicInteger();
    // 趋势采样只需要平均活跃时长，维护 startTime 总和可避免每秒遍历全部活跃连接。
    private final AtomicLong webSocketActiveSessionStartTotalMs = new AtomicLong();
    private final AtomicInteger activeSseSessions = new AtomicInteger();
    private final AtomicInteger peakSseSessions = new AtomicInteger();
    // 实时报告仍可读取 session map 生成明细，趋势路径只走 O(1) 聚合。
    private final AtomicLong sseActiveSessionStartTotalMs = new AtomicLong();

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
        activeWebSocketSessions.set(0);
        peakWebSocketSessions.set(0);
        webSocketActiveSessionStartTotalMs.set(0);
        activeSseSessions.set(0);
        peakSseSessions.set(0);
        sseActiveSessionStartTotalMs.set(0);

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

    public void recordWebSocketSent(Object session) {
        recordWebSocketSent();
        incrementSessionCounter(webSocketSessionStarts, session, StreamCounter.SENT);
    }

    public void recordWebSocketReceived() {
        webSocketReceivedMessages.incrementAndGet();
    }

    public void recordWebSocketReceived(Object session) {
        recordWebSocketReceived();
        incrementSessionCounter(webSocketSessionStarts, session, StreamCounter.RECEIVED);
    }

    public void recordWebSocketMatched() {
        webSocketMatchedMessages.incrementAndGet();
    }

    public void recordWebSocketMatched(Object session) {
        recordWebSocketMatched();
        incrementSessionCounter(webSocketSessionStarts, session, StreamCounter.MATCHED);
    }

    public synchronized void recordWebSocketFirstMessageLatency(long latencyMs) {
        webSocketFirstMessageLatencyTotalMs.addAndGet(Math.max(0, latencyMs));
        webSocketFirstMessageLatencyCount.incrementAndGet();
    }

    public void recordWebSocketFirstMessageLatency(Object session, long latencyMs) {
        recordWebSocketFirstMessageLatency(latencyMs);
        recordSessionLatency(webSocketSessionStarts, session, latencyMs);
    }

    public void recordWebSocketSessionStart(Object session, long startTimeMs) {
        recordWebSocketSessionStart(session, startTimeMs, "", "");
    }

    public void recordWebSocketSessionStart(Object session, long startTimeMs, String apiId, String apiName) {
        if (session != null) {
            StreamSessionMetrics previous = webSocketSessionStarts.putIfAbsent(
                    session,
                    new StreamSessionMetrics(startTimeMs, apiId, apiName)
            );
            if (previous == null) {
                webSocketActiveSessionStartTotalMs.addAndGet(startTimeMs);
                updatePeak(peakWebSocketSessions, activeWebSocketSessions.incrementAndGet());
            }
        }
    }

    public void recordWebSocketSessionEnd(Object session) {
        if (session == null) {
            return;
        }
        StreamSessionMetrics removed = webSocketSessionStarts.remove(session);
        if (removed != null) {
            webSocketActiveSessionStartTotalMs.addAndGet(-removed.startTimeMs);
            decrementActive(activeWebSocketSessions);
        }
    }

    public void recordSseReceived() {
        sseReceivedMessages.incrementAndGet();
    }

    public void recordSseReceived(Object session) {
        recordSseReceived();
        incrementSessionCounter(sseSessionStarts, session, StreamCounter.RECEIVED);
    }

    public void recordSseMatched() {
        sseMatchedMessages.incrementAndGet();
    }

    public void recordSseMatched(Object session) {
        recordSseMatched();
        incrementSessionCounter(sseSessionStarts, session, StreamCounter.MATCHED);
    }

    public synchronized void recordSseFirstMessageLatency(long latencyMs) {
        sseFirstMessageLatencyTotalMs.addAndGet(Math.max(0, latencyMs));
        sseFirstMessageLatencyCount.incrementAndGet();
    }

    public void recordSseFirstMessageLatency(Object session, long latencyMs) {
        recordSseFirstMessageLatency(latencyMs);
        recordSessionLatency(sseSessionStarts, session, latencyMs);
    }

    public void recordSseSessionStart(Object session, long startTimeMs) {
        recordSseSessionStart(session, startTimeMs, "", "");
    }

    public void recordSseSessionStart(Object session, long startTimeMs, String apiId, String apiName) {
        if (session != null) {
            StreamSessionMetrics previous = sseSessionStarts.putIfAbsent(
                    session,
                    new StreamSessionMetrics(startTimeMs, apiId, apiName)
            );
            if (previous == null) {
                sseActiveSessionStartTotalMs.addAndGet(startTimeMs);
                updatePeak(peakSseSessions, activeSseSessions.incrementAndGet());
            }
        }
    }

    public void recordSseSessionEnd(Object session) {
        if (session == null) {
            return;
        }
        StreamSessionMetrics removed = sseSessionStarts.remove(session);
        if (removed != null) {
            sseActiveSessionStartTotalMs.addAndGet(-removed.startTimeMs);
            decrementActive(activeSseSessions);
        }
    }

    public LiveSnapshot liveSnapshot(long nowMs) {
        return new LiveSnapshot(
                liveProtocolSnapshot(webSocketSessionStarts, nowMs),
                liveProtocolSnapshot(sseSessionStarts, nowMs)
        );
    }

    /**
     * 读取流式协议实时窗口并推进上次计数，QPS/消息速率类指标不能重复读取。
     */
    public synchronized Sample drainWindow(long nowMs) {
        long previousSampleTimeMs = lastSampleTimeMs.getAndSet(nowMs);
        long elapsedMs = Math.max(MIN_RATE_SAMPLE_INTERVAL_MS, nowMs - previousSampleTimeMs);
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
        int webSocketActiveSessionCount = peakAndReset(activeWebSocketSessions, peakWebSocketSessions);
        int sseActiveSessionCount = peakAndReset(activeSseSessions, peakSseSessions);

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
                webSocketActiveSessionCount,
                activeDuration(activeWebSocketSessions.get(), webSocketActiveSessionStartTotalMs.get(), nowMs),
                rate(currentSseReceived - lastSseReceivedMessages.getAndSet(currentSseReceived), seconds),
                rate(currentSseMatched - lastSseMatchedMessages.getAndSet(currentSseMatched), seconds),
                average(
                        currentSseLatencyTotal - lastSseFirstMessageLatencyTotalMs.getAndSet(currentSseLatencyTotal),
                        sseLatencyCountDelta
                ),
                sseActiveSessionCount,
                activeDuration(activeSseSessions.get(), sseActiveSessionStartTotalMs.get(), nowMs)
        );
    }

    private static double rate(long count, double seconds) {
        return seconds > 0 ? round(Math.max(0, count) / seconds) : 0;
    }

    private static double average(long total, long count) {
        return count > 0 ? round((double) Math.max(0, total) / count) : Double.NaN;
    }

    private static double activeDuration(int activeSessions, long activeStartTotalMs, long nowMs) {
        int count = Math.max(0, activeSessions);
        if (count == 0) {
            return 0;
        }
        long total = Math.max(0, nowMs * (long) count - activeStartTotalMs);
        return round((double) total / count);
    }

    private static void updatePeak(AtomicInteger peak, int activeSessions) {
        int observed;
        do {
            observed = peak.get();
            if (activeSessions <= observed) {
                return;
            }
        } while (!peak.compareAndSet(observed, activeSessions));
    }

    private static void decrementActive(AtomicInteger activeSessions) {
        activeSessions.updateAndGet(current -> Math.max(0, current - 1));
    }

    private static int peakAndReset(AtomicInteger activeSessions, AtomicInteger peakSessions) {
        int current = Math.max(0, activeSessions.get());
        int peak = peakSessions.getAndSet(current);
        return Math.max(current, peak);
    }

    private static LiveProtocolSnapshot liveProtocolSnapshot(Map<Object, StreamSessionMetrics> sessions, long nowMs) {
        if (sessions.isEmpty()) {
            return LiveProtocolSnapshot.empty();
        }
        long earliestStart = Long.MAX_VALUE;
        int activeSessions = 0;
        long activeDurationTotal = 0;
        long sent = 0;
        long received = 0;
        long matched = 0;
        long firstLatencyTotal = 0;
        long firstLatencyCount = 0;
        Map<String, MutableLiveApiSnapshot> apiSnapshots = new HashMap<>();

        for (StreamSessionMetrics session : sessions.values()) {
            if (session == null) {
                continue;
            }
            earliestStart = Math.min(earliestStart, session.startTimeMs);
            activeSessions++;
            activeDurationTotal += Math.max(0, nowMs - session.startTimeMs);
            StreamSessionMetrics.SessionSnapshot snapshot = session.snapshot();
            sent += snapshot.sentMessages();
            received += snapshot.receivedMessages();
            matched += snapshot.matchedMessages();
            long latencyCount = snapshot.firstMessageLatencyCount();
            if (latencyCount > 0) {
                firstLatencyCount += latencyCount;
                firstLatencyTotal += snapshot.firstMessageLatencyTotalMs();
            }
            if (!session.apiId.isBlank() || !session.apiName.isBlank()) {
                String apiKey = session.apiId.isBlank() ? session.apiName : session.apiId;
                apiSnapshots.computeIfAbsent(apiKey, ignored -> new MutableLiveApiSnapshot(session.apiId, session.apiName))
                        .record(nowMs, session, snapshot);
            }
        }

        long elapsedMs = earliestStart == Long.MAX_VALUE ? 1 : Math.max(1, nowMs - earliestStart);
        double seconds = elapsedMs / 1000.0;
        long avgFirstLatency = firstLatencyCount == 0 ? 0 : firstLatencyTotal / firstLatencyCount;
        long avgActiveDuration = activeSessions == 0 ? 0 : activeDurationTotal / activeSessions;
        return new LiveProtocolSnapshot(
                activeSessions,
                sent,
                received,
                matched,
                rate(sent, seconds),
                rate(received, seconds),
                rate(matched, seconds),
                avgFirstLatency,
                singleValueStats(avgFirstLatency),
                singleValueStats(avgActiveDuration),
                apiSnapshots.values().stream()
                        .map(snapshot -> snapshot.toSnapshot(nowMs))
                        .toList()
        );
    }

    private static PerformanceStatsSnapshot.DurationStats singleValueStats(double value) {
        long normalized = Math.max(0, Math.round(value));
        return new PerformanceStatsSnapshot.DurationStats(
                normalized,
                normalized,
                normalized,
                normalized,
                normalized,
                normalized
        );
    }

    private static void incrementSessionCounter(Map<Object, StreamSessionMetrics> sessions,
                                                Object session,
                                                StreamCounter counter) {
        if (session == null || counter == null) {
            return;
        }
        StreamSessionMetrics sessionMetrics = sessions.get(session);
        if (sessionMetrics == null) {
            return;
        }
        switch (counter) {
            case SENT -> sessionMetrics.incrementSent();
            case RECEIVED -> sessionMetrics.incrementReceived();
            case MATCHED -> sessionMetrics.incrementMatched();
        }
    }

    private static void recordSessionLatency(Map<Object, StreamSessionMetrics> sessions,
                                             Object session,
                                             long latencyMs) {
        if (session == null) {
            return;
        }
        StreamSessionMetrics sessionMetrics = sessions.get(session);
        if (sessionMetrics == null) {
            return;
        }
        sessionMetrics.recordFirstMessageLatency(latencyMs);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record Sample(
            double webSocketSentRate,
            double webSocketReceivedRate,
            double webSocketMatchedRate,
            double webSocketFirstMessageLatencyMs,
            int webSocketActiveSessions,
            double webSocketActiveSessionDurationMs,
            double sseReceivedRate,
            double sseMatchedRate,
            double sseFirstMessageLatencyMs,
            int sseActiveSessions,
            double sseActiveSessionDurationMs
    ) {
        public static Sample empty() {
            return new Sample(0, 0, 0, Double.NaN, 0, 0, 0, 0, Double.NaN, 0, 0);
        }
    }

    public record LiveSnapshot(
            LiveProtocolSnapshot webSocket,
            LiveProtocolSnapshot sse
    ) {
        public static LiveSnapshot empty() {
            return new LiveSnapshot(LiveProtocolSnapshot.empty(), LiveProtocolSnapshot.empty());
        }
    }

    public record LiveProtocolSnapshot(
            int activeSessions,
            long sentMessages,
            long receivedMessages,
            long matchedMessages,
            double sendRate,
            double receiveRate,
            double matchedRate,
            long avgFirstMessageLatencyMs,
            PerformanceStatsSnapshot.DurationStats firstMessageLatencyStats,
            PerformanceStatsSnapshot.DurationStats activeDurationStats,
            List<LiveApiSnapshot> apiSnapshots
    ) {
        public boolean hasData() {
            return activeSessions > 0 || sentMessages > 0 || receivedMessages > 0 || matchedMessages > 0;
        }

        private static LiveProtocolSnapshot empty() {
            return new LiveProtocolSnapshot(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    PerformanceStatsSnapshot.DurationStats.empty(),
                    PerformanceStatsSnapshot.DurationStats.empty(),
                    List.of()
            );
        }
    }

    public record LiveApiSnapshot(
            String apiId,
            String apiName,
            LiveProtocolSnapshot metrics
    ) {
    }

    private enum StreamCounter {
        SENT,
        RECEIVED,
        MATCHED
    }

    private static final class StreamSessionMetrics {
        private final long startTimeMs;
        private final String apiId;
        private final String apiName;
        private long sentMessages;
        private long receivedMessages;
        private long matchedMessages;
        private long firstMessageLatencyTotalMs;
        private long firstMessageLatencyCount;

        private StreamSessionMetrics(long startTimeMs, String apiId, String apiName) {
            this.startTimeMs = startTimeMs;
            this.apiId = apiId == null ? "" : apiId;
            this.apiName = apiName == null ? "" : apiName;
        }

        private synchronized void incrementSent() {
            sentMessages++;
        }

        private synchronized void incrementReceived() {
            receivedMessages++;
        }

        private synchronized void incrementMatched() {
            matchedMessages++;
        }

        private synchronized void recordFirstMessageLatency(long latencyMs) {
            firstMessageLatencyTotalMs += Math.max(0, latencyMs);
            firstMessageLatencyCount++;
        }

        private synchronized SessionSnapshot snapshot() {
            return new SessionSnapshot(
                    sentMessages,
                    receivedMessages,
                    matchedMessages,
                    firstMessageLatencyTotalMs,
                    firstMessageLatencyCount
            );
        }

        private record SessionSnapshot(
                long sentMessages,
                long receivedMessages,
                long matchedMessages,
                long firstMessageLatencyTotalMs,
                long firstMessageLatencyCount
        ) {
        }
    }

    private static final class MutableLiveApiSnapshot {
        private final String apiId;
        private final String apiName;
        private long earliestStart = Long.MAX_VALUE;
        private int activeSessions;
        private long activeDurationTotal;
        private long sent;
        private long received;
        private long matched;
        private long firstLatencyTotal;
        private long firstLatencyCount;

        private MutableLiveApiSnapshot(String apiId, String apiName) {
            this.apiId = apiId == null ? "" : apiId;
            this.apiName = apiName == null ? "" : apiName;
        }

        private void record(long nowMs, StreamSessionMetrics session, StreamSessionMetrics.SessionSnapshot snapshot) {
            earliestStart = Math.min(earliestStart, session.startTimeMs);
            activeSessions++;
            activeDurationTotal += Math.max(0, nowMs - session.startTimeMs);
            sent += snapshot.sentMessages();
            received += snapshot.receivedMessages();
            matched += snapshot.matchedMessages();
            long latencyCount = snapshot.firstMessageLatencyCount();
            if (latencyCount > 0) {
                firstLatencyCount += latencyCount;
                firstLatencyTotal += snapshot.firstMessageLatencyTotalMs();
            }
        }

        private LiveApiSnapshot toSnapshot(long nowMs) {
            long elapsedMs = earliestStart == Long.MAX_VALUE ? 1 : Math.max(1, nowMs - earliestStart);
            double seconds = elapsedMs / 1000.0;
            long avgFirstLatency = firstLatencyCount == 0 ? 0 : firstLatencyTotal / firstLatencyCount;
            long avgActiveDuration = activeSessions == 0 ? 0 : activeDurationTotal / activeSessions;
            return new LiveApiSnapshot(
                    apiId,
                    apiName,
                    new LiveProtocolSnapshot(
                            activeSessions,
                            sent,
                            received,
                            matched,
                            rate(sent, seconds),
                            rate(received, seconds),
                            rate(matched, seconds),
                            avgFirstLatency,
                            singleValueStats(avgFirstLatency),
                            singleValueStats(avgActiveDuration),
                            List.of()
                    )
            );
        }
    }
}

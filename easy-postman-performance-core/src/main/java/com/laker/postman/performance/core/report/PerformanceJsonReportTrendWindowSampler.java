package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;

/**
 * 从 worker/master JSON 报表的累计值中切出单个趋势采样窗口，GUI/CLI/master 只负责传入当前报表。
 */
public final class PerformanceJsonReportTrendWindowSampler {
    private long lastSampleAtMs;
    private long lastTotalRequests;
    private long lastFailedRequests;
    private int lastWindowActiveUsers;
    private final ProtocolCounter http = new ProtocolCounter();
    private final ProtocolCounter webSocket = new ProtocolCounter();
    private final ProtocolCounter sse = new ProtocolCounter();

    public void reset(long nowMs) {
        lastSampleAtMs = Math.max(0L, nowMs);
        lastTotalRequests = 0L;
        lastFailedRequests = 0L;
        lastWindowActiveUsers = 0;
        http.reset();
        webSocket.reset();
        sse.reset();
    }

    /**
     * 对 worker 累计报表做差分并推进内部游标，结果只代表本次 poll 到上次 poll 之间的窗口。
     */
    public PerformanceTrendSnapshot drainReportDelta(int activeUsers,
                                                     int activeWebSocketConnections,
                                                     int activeSseStreams,
                                                     long totalRequests,
                                                     long failedRequests,
                                                     PerformanceJsonReport report,
                                                     long nowMs) {
        long elapsedMs = elapsedMs(nowMs);
        long sampleDelta = positiveDelta(totalRequests, lastTotalRequests);
        long failureDelta = positiveDelta(failedRequests, lastFailedRequests);
        PerformanceTrendSnapshot.ProtocolWindowMetrics httpMetrics =
                http.drainWindow(protocolTotal(report, PerformanceProtocol.HTTP), elapsedMs);
        boolean streamReportHasCompletedSamples = reportCompletedSamples(report) > 0;
        PerformanceTrendSnapshot.ProtocolWindowMetrics webSocketMetrics =
                webSocket.drainWindow(protocolTotal(report, PerformanceProtocol.WEBSOCKET),
                        elapsedMs,
                        streamReportHasCompletedSamples);
        PerformanceTrendSnapshot.ProtocolWindowMetrics sseMetrics =
                sse.drainWindow(protocolTotal(report, PerformanceProtocol.SSE), elapsedMs, streamReportHasCompletedSamples);
        PerformanceTrendSnapshot.ProtocolWindowMetrics overviewMetrics =
                overview(sampleDelta, failureDelta, elapsedMs, webSocketMetrics, sseMetrics);
        int snapshotActiveUsers = resolveWindowActiveUsers(activeUsers, overviewMetrics, httpMetrics, webSocketMetrics, sseMetrics);

        lastSampleAtMs = Math.max(lastSampleAtMs, nowMs);
        lastTotalRequests = Math.max(lastTotalRequests, totalRequests);
        lastFailedRequests = Math.max(lastFailedRequests, failedRequests);
        if (activeUsers > 0) {
            lastWindowActiveUsers = Math.max(lastWindowActiveUsers, activeUsers);
        } else if (!hasWindowActivity(overviewMetrics, httpMetrics, webSocketMetrics, sseMetrics)) {
            lastWindowActiveUsers = 0;
        }

        return new PerformanceTrendSnapshot(
                snapshotActiveUsers,
                Math.max(0, activeWebSocketConnections),
                Math.max(0, activeSseStreams),
                overviewMetrics,
                httpMetrics,
                webSocketMetrics,
                sseMetrics
        );
    }

    private int resolveWindowActiveUsers(int activeUsers,
                                         PerformanceTrendSnapshot.ProtocolWindowMetrics overviewMetrics,
                                         PerformanceTrendSnapshot.ProtocolWindowMetrics httpMetrics,
                                         PerformanceTrendSnapshot.ProtocolWindowMetrics webSocketMetrics,
                                         PerformanceTrendSnapshot.ProtocolWindowMetrics sseMetrics) {
        if (activeUsers > 0) {
            lastWindowActiveUsers = Math.max(lastWindowActiveUsers, activeUsers);
            return Math.max(0, activeUsers);
        }
        if (hasWindowActivity(overviewMetrics, httpMetrics, webSocketMetrics, sseMetrics)) {
            // 远程 worker 可能在本轮 poll 时已经完成，activeUsers 已归零，但本轮报表仍有刚完成的样本。
            return Math.max(0, lastWindowActiveUsers);
        }
        return 0;
    }

    private boolean hasWindowActivity(PerformanceTrendSnapshot.ProtocolWindowMetrics... metrics) {
        if (metrics == null) {
            return false;
        }
        for (PerformanceTrendSnapshot.ProtocolWindowMetrics metric : metrics) {
            if (metric != null && (metric.samples() > 0
                    || metric.sentMessages() > 0
                    || metric.receivedMessages() > 0
                    || metric.matchedMessages() > 0)) {
                return true;
            }
        }
        return false;
    }

    private long elapsedMs(long nowMs) {
        if (lastSampleAtMs <= 0 || nowMs <= lastSampleAtMs) {
            return 1000L;
        }
        return Math.max(1L, nowMs - lastSampleAtMs);
    }

    private PerformanceTrendSnapshot.ProtocolWindowMetrics overview(
            long sampleDelta,
            long failureDelta,
            long elapsedMs,
            PerformanceTrendSnapshot.ProtocolWindowMetrics webSocketMetrics,
            PerformanceTrendSnapshot.ProtocolWindowMetrics sseMetrics) {
        int samples = clampToInt(sampleDelta);
        int failures = clampToInt(failureDelta);
        return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                samples,
                failures,
                samples == 0 ? 0.0 : failures * 100.0 / samples,
                rate(sampleDelta, elapsedMs),
                Double.NaN,
                webSocketMetrics.sentMessages() + sseMetrics.sentMessages(),
                webSocketMetrics.receivedMessages() + sseMetrics.receivedMessages(),
                webSocketMetrics.matchedMessages() + sseMetrics.matchedMessages(),
                webSocketMetrics.sentRate() + sseMetrics.sentRate(),
                webSocketMetrics.receivedRate() + sseMetrics.receivedRate(),
                webSocketMetrics.matchedRate() + sseMetrics.matchedRate(),
                Double.NaN
        );
    }

    private PerformanceJsonReportApi protocolTotal(PerformanceJsonReport report, PerformanceProtocol protocol) {
        if (report == null || report.getProtocols() == null || protocol == null) {
            return null;
        }
        PerformanceJsonReportProtocol protocolReport = report.getProtocols().get(protocol.name());
        return protocolReport == null ? null : protocolReport.getTotal();
    }

    private long reportCompletedSamples(PerformanceJsonReport report) {
        if (report == null || report.getSummary() == null) {
            return 0L;
        }
        return Math.max(0L, report.getSummary().getTotalRequests());
    }

    private static long positiveDelta(long current, long previous) {
        return Math.max(0L, current - previous);
    }

    private static double rate(long delta, long elapsedMs) {
        return elapsedMs <= 0 ? 0.0 : delta * 1000.0 / elapsedMs;
    }

    private static int clampToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static final class ProtocolCounter {
        private long total;
        private long failed;
        private long sentMessages;
        private long receivedMessages;
        private long matchedMessages;
        private long durationTotalMs;
        private long firstLatencyTotalMs;

        void reset() {
            total = 0L;
            failed = 0L;
            sentMessages = 0L;
            receivedMessages = 0L;
            matchedMessages = 0L;
            durationTotalMs = 0L;
            firstLatencyTotalMs = 0L;
        }

        PerformanceTrendSnapshot.ProtocolWindowMetrics drainWindow(PerformanceJsonReportApi api, long elapsedMs) {
            return drainWindow(api, elapsedMs, true);
        }

        PerformanceTrendSnapshot.ProtocolWindowMetrics drainWindow(PerformanceJsonReportApi api,
                                                              long elapsedMs,
                                                              boolean countCompletedSamples) {
            long currentTotal = api == null ? total : api.getTotal();
            long currentFailed = api == null ? failed : api.getFailed();
            long currentSent = api == null || api.getStream() == null ? sentMessages : api.getStream().getSentMessages();
            long currentReceived = api == null || api.getStream() == null
                    ? receivedMessages
                    : api.getStream().getReceivedMessages();
            long currentMatched = api == null || api.getStream() == null
                    ? matchedMessages
                    : api.getStream().getMatchedMessages();
            long currentDurationTotalMs = durationTotal(api, currentTotal);
            long currentFirstLatencyTotalMs = durationTotal(api == null ? null : api.getFirstMessageLatencyMs(), currentTotal);

            long totalDelta = countCompletedSamples ? positiveDelta(currentTotal, total) : 0L;
            long failedDelta = countCompletedSamples ? positiveDelta(currentFailed, failed) : 0L;
            long sentDelta = positiveDelta(currentSent, sentMessages);
            long receivedDelta = positiveDelta(currentReceived, receivedMessages);
            long matchedDelta = positiveDelta(currentMatched, matchedMessages);
            long durationDeltaMs = countCompletedSamples ? positiveDelta(currentDurationTotalMs, durationTotalMs) : 0L;
            long firstLatencyDeltaMs = countCompletedSamples ? positiveDelta(currentFirstLatencyTotalMs, firstLatencyTotalMs) : 0L;

            if (countCompletedSamples) {
                total = Math.max(total, currentTotal);
                failed = Math.max(failed, currentFailed);
                durationTotalMs = Math.max(durationTotalMs, currentDurationTotalMs);
                firstLatencyTotalMs = Math.max(firstLatencyTotalMs, currentFirstLatencyTotalMs);
            }
            sentMessages = Math.max(sentMessages, currentSent);
            receivedMessages = Math.max(receivedMessages, currentReceived);
            matchedMessages = Math.max(matchedMessages, currentMatched);

            int samples = clampToInt(totalDelta);
            int failures = clampToInt(failedDelta);
            return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                    samples,
                    failures,
                    samples == 0 ? 0.0 : failures * 100.0 / samples,
                    rate(totalDelta, elapsedMs),
                    averageDuration(api == null ? null : api.getDurationMs(), totalDelta, durationDeltaMs),
                    clampToInt(sentDelta),
                    clampToInt(receivedDelta),
                    clampToInt(matchedDelta),
                    rate(sentDelta, elapsedMs),
                    rate(receivedDelta, elapsedMs),
                    rate(matchedDelta, elapsedMs),
                    averageDuration(api == null ? null : api.getFirstMessageLatencyMs(), totalDelta, firstLatencyDeltaMs)
            );
        }

        private long durationTotal(PerformanceJsonReportApi api, long sampleCount) {
            return api == null ? 0L : durationTotal(api.getDurationMs(), sampleCount);
        }

        private long durationTotal(PerformanceJsonReportDuration duration, long sampleCount) {
            if (duration == null || sampleCount <= 0 || duration.getAvg() <= 0) {
                return 0L;
            }
            return duration.getAvg() * sampleCount;
        }

        private double averageDuration(PerformanceJsonReportDuration duration, long sampleDelta, long durationDeltaMs) {
            if (duration == null) {
                return Double.NaN;
            }
            if (sampleDelta > 0) {
                return (double) Math.max(0L, durationDeltaMs) / sampleDelta;
            }
            return duration.getAvg();
        }
    }
}

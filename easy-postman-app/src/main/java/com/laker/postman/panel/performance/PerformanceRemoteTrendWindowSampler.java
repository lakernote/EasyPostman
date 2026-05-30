package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportApi;
import com.laker.postman.performance.core.report.PerformanceJsonReportProtocol;

final class PerformanceRemoteTrendWindowSampler {
    private long lastSampleAtMs;
    private long lastTotalRequests;
    private long lastFailedRequests;
    private final ProtocolCounter http = new ProtocolCounter();
    private final ProtocolCounter webSocket = new ProtocolCounter();
    private final ProtocolCounter sse = new ProtocolCounter();

    void reset(long nowMs) {
        lastSampleAtMs = Math.max(0L, nowMs);
        lastTotalRequests = 0L;
        lastFailedRequests = 0L;
        http.reset();
        webSocket.reset();
        sse.reset();
    }

    PerformanceTrendSnapshot sample(int activeUsers,
                                    long totalRequests,
                                    long failedRequests,
                                    PerformanceJsonReport report,
                                    long nowMs) {
        long elapsedMs = elapsedMs(nowMs);
        long sampleDelta = positiveDelta(totalRequests, lastTotalRequests);
        long failureDelta = positiveDelta(failedRequests, lastFailedRequests);
        PerformanceTrendSnapshot.ProtocolWindowMetrics httpMetrics =
                http.sample(protocolTotal(report, PerformanceProtocol.HTTP), elapsedMs);
        PerformanceTrendSnapshot.ProtocolWindowMetrics webSocketMetrics =
                webSocket.sample(protocolTotal(report, PerformanceProtocol.WEBSOCKET), elapsedMs);
        PerformanceTrendSnapshot.ProtocolWindowMetrics sseMetrics =
                sse.sample(protocolTotal(report, PerformanceProtocol.SSE), elapsedMs);

        lastSampleAtMs = Math.max(lastSampleAtMs, nowMs);
        lastTotalRequests = Math.max(lastTotalRequests, totalRequests);
        lastFailedRequests = Math.max(lastFailedRequests, failedRequests);

        return new PerformanceTrendSnapshot(
                Math.max(0, activeUsers),
                0,
                0,
                overview(sampleDelta, failureDelta, elapsedMs, webSocketMetrics, sseMetrics),
                httpMetrics,
                webSocketMetrics,
                sseMetrics
        );
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

        void reset() {
            total = 0L;
            failed = 0L;
            sentMessages = 0L;
            receivedMessages = 0L;
            matchedMessages = 0L;
        }

        PerformanceTrendSnapshot.ProtocolWindowMetrics sample(PerformanceJsonReportApi api, long elapsedMs) {
            long currentTotal = api == null ? total : api.getTotal();
            long currentFailed = api == null ? failed : api.getFailed();
            long currentSent = api == null || api.getStream() == null ? sentMessages : api.getStream().getSentMessages();
            long currentReceived = api == null || api.getStream() == null ? receivedMessages : api.getStream().getReceivedMessages();
            long currentMatched = api == null || api.getStream() == null ? matchedMessages : api.getStream().getMatchedMessages();

            long totalDelta = positiveDelta(currentTotal, total);
            long failedDelta = positiveDelta(currentFailed, failed);
            long sentDelta = positiveDelta(currentSent, sentMessages);
            long receivedDelta = positiveDelta(currentReceived, receivedMessages);
            long matchedDelta = positiveDelta(currentMatched, matchedMessages);

            total = Math.max(total, currentTotal);
            failed = Math.max(failed, currentFailed);
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
                    api == null || api.getDurationMs() == null ? Double.NaN : api.getDurationMs().getAvg(),
                    clampToInt(sentDelta),
                    clampToInt(receivedDelta),
                    clampToInt(matchedDelta),
                    rate(sentDelta, elapsedMs),
                    rate(receivedDelta, elapsedMs),
                    rate(matchedDelta, elapsedMs),
                    api == null || api.getFirstMessageLatencyMs() == null
                            ? Double.NaN
                            : api.getFirstMessageLatencyMs().getAvg()
            );
        }
    }
}

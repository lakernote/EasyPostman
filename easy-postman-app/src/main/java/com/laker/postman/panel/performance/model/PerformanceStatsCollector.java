package com.laker.postman.panel.performance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class PerformanceStatsCollector {

    private static final int[] DURATION_BUCKET_UPPER_BOUNDS = buildDurationBucketUpperBounds();

    private final EnumMap<PerformanceProtocol, Map<String, MutableStats>> apiStatsByProtocol =
            new EnumMap<>(PerformanceProtocol.class);
    private final EnumMap<PerformanceProtocol, MutableStats> protocolTotals = new EnumMap<>(PerformanceProtocol.class);
    private final MutableStats overallStats = new MutableStats("", PerformanceProtocol.HTTP);
    private final EnumMap<PerformanceProtocol, MutableStats> trendProtocolStats = new EnumMap<>(PerformanceProtocol.class);
    private MutableStats trendOverallStats = new MutableStats("", PerformanceProtocol.HTTP);

    public synchronized void record(RequestResult result) {
        if (result == null) {
            return;
        }
        PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
        String apiId = result.apiId == null ? "" : result.apiId;
        Map<String, MutableStats> protocolApiStats =
                apiStatsByProtocol.computeIfAbsent(protocol, ignored -> new HashMap<>());

        protocolApiStats.computeIfAbsent(apiId, ignored -> new MutableStats(apiId, protocol)).record(result);
        protocolTotals.computeIfAbsent(protocol, ignored -> new MutableStats("", protocol)).record(result);
        overallStats.record(result);
        trendProtocolStats.computeIfAbsent(protocol, ignored -> new MutableStats("", protocol)).record(result);
        trendOverallStats.record(result);
    }

    public synchronized PerformanceStatsSnapshot snapshot() {
        List<PerformanceStatsSnapshot.ApiSummary> summaries = new ArrayList<>();
        for (Map<String, MutableStats> statsByApi : apiStatsByProtocol.values()) {
            for (MutableStats stats : statsByApi.values()) {
                summaries.add(stats.toSummary(ApiMetadata.getName(stats.apiId)));
            }
        }

        EnumMap<PerformanceProtocol, PerformanceStatsSnapshot.ApiSummary> totals = new EnumMap<>(PerformanceProtocol.class);
        for (Map.Entry<PerformanceProtocol, MutableStats> entry : protocolTotals.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().toSummary(""));
        }

        return new PerformanceStatsSnapshot(
                summaries,
                totals,
                overallStats.total,
                overallStats.success,
                0
        );
    }

    public synchronized PerformanceTrendSnapshot sampleTrendSnapshot(long nowMs,
                                                                     int activeUsers,
                                                                     int activeWebSocketConnections,
                                                                     int activeSseStreams,
                                                                     long samplingIntervalMs,
                                                                     PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        PerformanceTrendSnapshot snapshot = new PerformanceTrendSnapshot(
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                toWindowMetrics(trendOverallStats, null, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(trendProtocolStats.get(PerformanceProtocol.HTTP),
                        PerformanceProtocol.HTTP, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(trendProtocolStats.get(PerformanceProtocol.WEBSOCKET),
                        PerformanceProtocol.WEBSOCKET, samplingIntervalMs, realtimeMetrics),
                toWindowMetrics(trendProtocolStats.get(PerformanceProtocol.SSE),
                        PerformanceProtocol.SSE, samplingIntervalMs, realtimeMetrics)
        );
        trendProtocolStats.clear();
        trendOverallStats = new MutableStats("", PerformanceProtocol.HTTP);
        return snapshot;
    }

    public synchronized void clear() {
        apiStatsByProtocol.clear();
        protocolTotals.clear();
        overallStats.clear();
        trendProtocolStats.clear();
        trendOverallStats = new MutableStats("", PerformanceProtocol.HTTP);
    }

    private static PerformanceTrendSnapshot.ProtocolWindowMetrics toWindowMetrics(
            MutableStats stats,
            PerformanceProtocol protocol,
            long samplingIntervalMs,
            PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        if (stats == null || stats.total == 0) {
            return realtimeStreamMetrics(protocol, realtimeMetrics);
        }

        double seconds = Math.max(0.001, samplingIntervalMs / 1000.0);
        int samples = clampToInt(stats.total);
        int failures = clampToInt(stats.fail());
        double sentRate = round(stats.sentMessages / seconds);
        double receivedRate = round(stats.receivedMessages / seconds);
        double matchedRate = round(stats.matchedMessages / seconds);
        double avgDuration = stats.durations.avg();
        double avgFirstMessageLatency = stats.firstMessageLatencyCount == 0
                ? 0
                : round((double) stats.firstMessageLatencyTotal / stats.firstMessageLatencyCount);

        if (realtimeMetrics != null && protocol == PerformanceProtocol.WEBSOCKET) {
            sentRate = realtimeMetrics.webSocketSentRate();
            receivedRate = realtimeMetrics.webSocketReceivedRate();
            matchedRate = realtimeMetrics.webSocketMatchedRate();
            avgFirstMessageLatency = realtimeMetrics.webSocketFirstMessageLatencyMs();
            if (realtimeMetrics.webSocketActiveSessionDurationMs() > 0) {
                avgDuration = realtimeMetrics.webSocketActiveSessionDurationMs();
            }
        } else if (realtimeMetrics != null && protocol == PerformanceProtocol.SSE) {
            receivedRate = realtimeMetrics.sseReceivedRate();
            matchedRate = realtimeMetrics.sseMatchedRate();
            avgFirstMessageLatency = realtimeMetrics.sseFirstMessageLatencyMs();
            if (realtimeMetrics.sseActiveSessionDurationMs() > 0) {
                avgDuration = realtimeMetrics.sseActiveSessionDurationMs();
            }
        }

        return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                samples,
                failures,
                stats.total > 0 ? round((double) failures * 100 / stats.total) : 0,
                round(stats.total / seconds),
                avgDuration,
                clampToInt(stats.sentMessages),
                clampToInt(stats.receivedMessages),
                clampToInt(stats.matchedMessages),
                sentRate,
                receivedRate,
                matchedRate,
                avgFirstMessageLatency
        );
    }

    private static PerformanceTrendSnapshot.ProtocolWindowMetrics realtimeStreamMetrics(
            PerformanceProtocol protocol,
            PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        if (realtimeMetrics != null && protocol == PerformanceProtocol.WEBSOCKET) {
            return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                    0,
                    0,
                    0,
                    0,
                    realtimeMetrics.webSocketActiveSessionDurationMs(),
                    0,
                    0,
                    0,
                    realtimeMetrics.webSocketSentRate(),
                    realtimeMetrics.webSocketReceivedRate(),
                    realtimeMetrics.webSocketMatchedRate(),
                    realtimeMetrics.webSocketFirstMessageLatencyMs()
            );
        }
        if (realtimeMetrics != null && protocol == PerformanceProtocol.SSE) {
            return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                    0,
                    0,
                    0,
                    0,
                    realtimeMetrics.sseActiveSessionDurationMs(),
                    0,
                    0,
                    0,
                    0,
                    realtimeMetrics.sseReceivedRate(),
                    realtimeMetrics.sseMatchedRate(),
                    realtimeMetrics.sseFirstMessageLatencyMs()
            );
        }
        return new PerformanceTrendSnapshot.ProtocolWindowMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static int clampToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, value);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static int[] buildDurationBucketUpperBounds() {
        List<Integer> bounds = new ArrayList<>(16_500);
        addRange(bounds, 0, 1_000, 1);
        addRange(bounds, 1_010, 60_000, 10);
        addRange(bounds, 60_100, 600_000, 100);
        addRange(bounds, 601_000, 3_600_000, 1_000);
        bounds.add(Integer.MAX_VALUE);
        int[] result = new int[bounds.size()];
        for (int i = 0; i < bounds.size(); i++) {
            result[i] = bounds.get(i);
        }
        return result;
    }

    private static void addRange(List<Integer> bounds, int start, int end, int step) {
        for (int value = start; value <= end; value += step) {
            bounds.add(value);
        }
    }

    private static final class MutableStats {
        private final String apiId;
        private final PerformanceProtocol protocol;
        private final DurationHistogram durations = new DurationHistogram();
        private final Map<String, MutableLong> completionReasons = new HashMap<>();
        private long total;
        private long success;
        private long firstStart = Long.MAX_VALUE;
        private long lastEnd;
        private long sentMessages;
        private long receivedMessages;
        private long matchedMessages;
        private long firstMessageLatencyTotal;
        private long firstMessageLatencyCount;

        private MutableStats(String apiId, PerformanceProtocol protocol) {
            this.apiId = apiId;
            this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
        }

        private void record(RequestResult result) {
            total++;
            if (result.success) {
                success++;
            }
            firstStart = Math.min(firstStart, result.startTime);
            lastEnd = Math.max(lastEnd, result.endTime);
            durations.record(result.getResponseTime());
            sentMessages += Math.max(0, result.sentMessages);
            receivedMessages += Math.max(0, result.receivedMessages);
            matchedMessages += Math.max(0, result.matchedMessages);
            if (result.firstMessageLatencyMs >= 0) {
                firstMessageLatencyTotal += result.firstMessageLatencyMs;
                firstMessageLatencyCount++;
            }
            if (protocol != PerformanceProtocol.HTTP) {
                String reason = result.completionReason == null || result.completionReason.isBlank()
                        ? "-"
                        : result.completionReason;
                MutableLong reasonCount = completionReasons.get(reason);
                if (reasonCount == null) {
                    completionReasons.put(reason, new MutableLong(1));
                } else {
                    reasonCount.increment();
                }
            }
        }

        private long fail() {
            return total - success;
        }

        private void clear() {
            total = 0;
            success = 0;
            firstStart = Long.MAX_VALUE;
            lastEnd = 0;
            sentMessages = 0;
            receivedMessages = 0;
            matchedMessages = 0;
            firstMessageLatencyTotal = 0;
            firstMessageLatencyCount = 0;
            completionReasons.clear();
            durations.clear();
        }

        private PerformanceStatsSnapshot.ApiSummary toSummary(String name) {
            double spanSeconds = total == 0 ? 0 : Math.max(0.001, (lastEnd - firstStart) / 1000.0);
            double sendRate = spanSeconds > 0 ? round(sentMessages / spanSeconds) : 0;
            double receiveRate = spanSeconds > 0 ? round(receivedMessages / spanSeconds) : 0;
            double matchedRate = spanSeconds > 0 ? round(matchedMessages / spanSeconds) : 0;
            return new PerformanceStatsSnapshot.ApiSummary(
                    apiId,
                    name,
                    protocol,
                    total,
                    success,
                    fail(),
                    total > 0 ? success * 100.0 / total : 0,
                    spanSeconds > 0 ? round(total / spanSeconds) : 0,
                    durations.snapshot(),
                    sentMessages,
                    receivedMessages,
                    matchedMessages,
                    sendRate,
                    receiveRate,
                    matchedRate,
                    firstMessageLatencyCount == 0 ? 0 : firstMessageLatencyTotal / firstMessageLatencyCount,
                    topCompletionReason()
            );
        }

        private String topCompletionReason() {
            if (completionReasons.isEmpty()) {
                return "-";
            }
            return completionReasons.entrySet().stream()
                    .max((left, right) -> {
                        int countComparison = Long.compare(left.getValue().value, right.getValue().value);
                        return countComparison != 0
                                ? countComparison
                                : left.getKey().compareTo(right.getKey());
                    })
                    .map(Map.Entry::getKey)
                    .orElse("-");
        }
    }

    private static final class DurationHistogram {
        private final NavigableMap<Integer, MutableLong> countsByBucket = new TreeMap<>();
        private long count;
        private long sum;
        private long min = Long.MAX_VALUE;
        private long max;

        private void record(long durationMs) {
            long normalized = Math.max(0, durationMs);
            count++;
            sum += normalized;
            min = Math.min(min, normalized);
            max = Math.max(max, normalized);
            int bucket = bucketIndex(normalized);
            MutableLong bucketCount = countsByBucket.get(bucket);
            if (bucketCount == null) {
                countsByBucket.put(bucket, new MutableLong(1));
            } else {
                bucketCount.increment();
            }
        }

        private void clear() {
            countsByBucket.clear();
            count = 0;
            sum = 0;
            min = Long.MAX_VALUE;
            max = 0;
        }

        private long avg() {
            return count == 0 ? 0 : sum / count;
        }

        private PerformanceStatsSnapshot.DurationStats snapshot() {
            if (count == 0) {
                return PerformanceStatsSnapshot.DurationStats.empty();
            }
            return new PerformanceStatsSnapshot.DurationStats(
                    avg(),
                    min,
                    max,
                    percentile(0.90),
                    percentile(0.95),
                    percentile(0.99)
            );
        }

        private long percentile(double percentile) {
            if (count == 0) {
                return 0;
            }
            long target = Math.max(1, (long) Math.ceil(count * percentile));
            long seen = 0;
            for (Map.Entry<Integer, MutableLong> entry : countsByBucket.entrySet()) {
                seen += entry.getValue().value;
                if (seen >= target) {
                    long upperBound = DURATION_BUCKET_UPPER_BOUNDS[entry.getKey()];
                    return Math.min(upperBound, max);
                }
            }
            return max;
        }

        private static int bucketIndex(long durationMs) {
            int normalized = durationMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) durationMs;
            int low = 0;
            int high = DURATION_BUCKET_UPPER_BOUNDS.length - 1;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (normalized <= DURATION_BUCKET_UPPER_BOUNDS[mid]) {
                    high = mid;
                } else {
                    low = mid + 1;
                }
            }
            return low;
        }
    }

    private static final class MutableLong {
        private long value;

        private MutableLong(long value) {
            this.value = value;
        }

        private void increment() {
            value++;
        }
    }
}

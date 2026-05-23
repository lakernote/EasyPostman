package com.laker.postman.panel.performance.result;

import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceReportSnapshot;
import com.laker.postman.panel.performance.model.PerformanceStatsSnapshot;
import com.laker.postman.panel.performance.model.RequestResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PerformanceProtocolReportData {

    private static final double PERCENTILE_90 = 0.90;
    private static final double PERCENTILE_95 = 0.95;
    private static final double PERCENTILE_99 = 0.99;
    private static final int MIN_SAMPLE_SIZE_FOR_INTERPOLATION = 10;

    private final List<HttpReportRow> httpRows;
    private final List<StreamReportRow> webSocketRows;
    private final List<StreamReportRow> sseRows;

    private PerformanceProtocolReportData(List<HttpReportRow> httpRows,
                                          List<StreamReportRow> webSocketRows,
                                          List<StreamReportRow> sseRows) {
        this.httpRows = httpRows;
        this.webSocketRows = webSocketRows;
        this.sseRows = sseRows;
    }

    static PerformanceProtocolReportData fromResults(List<RequestResult> results, String totalRowName) {
        List<RequestResult> safeResults = results == null ? List.of() : results;
        return new PerformanceProtocolReportData(
                buildHttpRows(safeResults, totalRowName),
                buildStreamRows(safeResults, PerformanceProtocol.WEBSOCKET, totalRowName),
                buildStreamRows(safeResults, PerformanceProtocol.SSE, totalRowName)
        );
    }

    static PerformanceProtocolReportData fromStatsSnapshot(PerformanceStatsSnapshot snapshot, String totalRowName) {
        if (snapshot == null) {
            return new PerformanceProtocolReportData(List.of(), List.of(), List.of());
        }
        return new PerformanceProtocolReportData(
                buildHttpRows(snapshot, totalRowName),
                buildStreamRows(snapshot, PerformanceProtocol.WEBSOCKET, totalRowName),
                buildStreamRows(snapshot, PerformanceProtocol.SSE, totalRowName)
        );
    }

    static PerformanceProtocolReportData fromReportSnapshot(PerformanceReportSnapshot snapshot, String totalRowName) {
        if (snapshot == null) {
            return new PerformanceProtocolReportData(List.of(), List.of(), List.of());
        }
        return fromStatsSnapshot(snapshot.completedStats(), totalRowName);
    }

    List<HttpReportRow> httpRows() {
        return httpRows;
    }

    List<StreamReportRow> webSocketRows() {
        return webSocketRows;
    }

    List<StreamReportRow> sseRows() {
        return sseRows;
    }

    private static List<HttpReportRow> buildHttpRows(List<RequestResult> results, String totalRowName) {
        Map<String, List<RequestResult>> byApi = groupByApi(results, PerformanceProtocol.HTTP);
        List<HttpReportRow> rows = new ArrayList<>();
        for (List<RequestResult> apiResults : byApi.values()) {
            rows.add(toHttpRow(apiResults.get(0).getApiName(), apiResults));
        }
        sortByName(rows);
        if (!rows.isEmpty()) {
            rows.add(toHttpRow(totalRowName, flatten(byApi)));
        }
        return rows;
    }

    private static List<HttpReportRow> buildHttpRows(PerformanceStatsSnapshot snapshot, String totalRowName) {
        List<HttpReportRow> rows = new ArrayList<>();
        for (PerformanceStatsSnapshot.ApiSummary summary : snapshot.summaries()) {
            if (summary.protocol() == PerformanceProtocol.HTTP) {
                rows.add(toHttpRow(summary));
            }
        }
        sortByName(rows);
        PerformanceStatsSnapshot.ApiSummary total = snapshot.totalFor(PerformanceProtocol.HTTP, totalRowName);
        if (total != null && total.total() > 0) {
            rows.add(toHttpRow(total));
        }
        return rows;
    }

    private static List<StreamReportRow> buildStreamRows(List<RequestResult> results,
                                                         PerformanceProtocol protocol,
                                                         String totalRowName) {
        Map<String, List<RequestResult>> byApi = groupByApi(results, protocol);
        List<StreamReportRow> rows = new ArrayList<>();
        for (List<RequestResult> apiResults : byApi.values()) {
            rows.add(toStreamRow(apiResults.get(0).getApiName(), apiResults));
        }
        sortByName(rows);
        if (!rows.isEmpty()) {
            rows.add(toStreamRow(totalRowName, flatten(byApi)));
        }
        return rows;
    }

    private static List<StreamReportRow> buildStreamRows(PerformanceStatsSnapshot snapshot,
                                                         PerformanceProtocol protocol,
                                                         String totalRowName) {
        List<StreamReportRow> rows = new ArrayList<>();
        for (PerformanceStatsSnapshot.ApiSummary summary : snapshot.summaries()) {
            if (summary.protocol() == protocol) {
                rows.add(toStreamRow(summary));
            }
        }
        sortByName(rows);
        PerformanceStatsSnapshot.ApiSummary total = snapshot.totalFor(protocol, totalRowName);
        if (total != null && total.total() > 0) {
            rows.add(toStreamRow(total));
        }
        return rows;
    }

    private static Map<String, List<RequestResult>> groupByApi(List<RequestResult> results, PerformanceProtocol protocol) {
        Map<String, List<RequestResult>> byApi = new LinkedHashMap<>();
        for (RequestResult result : results) {
            if (result.protocol != protocol) {
                continue;
            }
            byApi.computeIfAbsent(result.apiId, key -> new ArrayList<>()).add(result);
        }
        return byApi;
    }

    private static List<RequestResult> flatten(Map<String, List<RequestResult>> byApi) {
        return byApi.values().stream().flatMap(List::stream).toList();
    }

    private static void sortByName(List<? extends NamedReportRow> rows) {
        rows.sort(Comparator.comparing(NamedReportRow::name, String.CASE_INSENSITIVE_ORDER));
    }

    private static HttpReportRow toHttpRow(String name, List<RequestResult> results) {
        int total = results.size();
        int success = countSuccess(results);
        int fail = total - success;
        List<Long> costs = results.stream().map(RequestResult::getResponseTime).toList();
        DurationStats stats = calculateDurationStats(costs);
        return new HttpReportRow(
                name,
                total,
                success,
                fail,
                successRate(total, success),
                calculateSamplesPerSecond(total, results),
                stats.avg(),
                stats.min(),
                stats.max(),
                stats.p90(),
                stats.p95(),
                stats.p99()
        );
    }

    private static HttpReportRow toHttpRow(PerformanceStatsSnapshot.ApiSummary summary) {
        PerformanceStatsSnapshot.DurationStats stats = summary.durationStats();
        return new HttpReportRow(
                summary.name(),
                summary.total(),
                summary.success(),
                summary.fail(),
                summary.successRate(),
                summary.samplesPerSecond(),
                stats.avg(),
                stats.min(),
                stats.max(),
                stats.p90(),
                stats.p95(),
                stats.p99()
        );
    }

    private static StreamReportRow toStreamRow(String name, List<RequestResult> results) {
        int total = results.size();
        int success = countSuccess(results);
        int fail = total - success;
        int sentMessages = 0;
        int receivedMessages = 0;
        int matchedMessages = 0;
        long firstLatencyTotal = 0;
        int firstLatencyCount = 0;
        List<Long> durations = new ArrayList<>();
        List<Long> firstLatencies = new ArrayList<>();

        for (RequestResult result : results) {
            sentMessages += Math.max(0, result.sentMessages);
            receivedMessages += Math.max(0, result.receivedMessages);
            matchedMessages += Math.max(0, result.matchedMessages);
            durations.add(result.getResponseTime());
            if (result.firstMessageLatencyMs >= 0) {
                firstLatencyTotal += result.firstMessageLatencyMs;
                firstLatencyCount++;
                firstLatencies.add(result.firstMessageLatencyMs);
            }
        }

        DurationStats stats = calculateDurationStats(durations);
        DurationStats firstLatencyStats = calculateDurationStats(firstLatencies);
        long avgFirstMessageLatency = firstLatencyCount == 0 ? 0 : firstLatencyTotal / firstLatencyCount;
        return new StreamReportRow(
                name,
                total,
                success,
                fail,
                successRate(total, success),
                sentMessages,
                receivedMessages,
                matchedMessages,
                calculateCountPerSecond(sentMessages, results),
                calculateCountPerSecond(receivedMessages, results),
                calculateCountPerSecond(matchedMessages, results),
                avgFirstMessageLatency,
                firstLatencyStats.p90(),
                firstLatencyStats.p95(),
                firstLatencyStats.p99(),
                stats.avg(),
                stats.p95()
        );
    }

    private static StreamReportRow toStreamRow(PerformanceStatsSnapshot.ApiSummary summary) {
        PerformanceStatsSnapshot.DurationStats stats = summary.durationStats();
        PerformanceStatsSnapshot.DurationStats firstLatencyStats = summary.firstMessageLatencyStats() == null
                ? PerformanceStatsSnapshot.DurationStats.empty()
                : summary.firstMessageLatencyStats();
        return new StreamReportRow(
                summary.name(),
                summary.total(),
                summary.success(),
                summary.fail(),
                summary.successRate(),
                summary.sentMessages(),
                summary.receivedMessages(),
                summary.matchedMessages(),
                summary.sendRate(),
                summary.receiveRate(),
                summary.matchedRate(),
                summary.avgFirstMessageLatencyMs(),
                firstLatencyStats.p90(),
                firstLatencyStats.p95(),
                firstLatencyStats.p99(),
                stats.avg(),
                stats.p95()
        );
    }

    private static int countSuccess(List<RequestResult> results) {
        int success = 0;
        for (RequestResult result : results) {
            if (result.success) {
                success++;
            }
        }
        return success;
    }

    private static double successRate(int total, int success) {
        return total > 0 ? success * 100.0 / total : 0;
    }

    private static double calculateSamplesPerSecond(int total, List<RequestResult> results) {
        return calculateCountPerSecond(total, results);
    }

    private static double calculateCountPerSecond(int count, List<RequestResult> results) {
        if (count == 0 || results.isEmpty()) {
            return 0;
        }
        long minStart = results.stream().mapToLong(result -> result.startTime).min().orElse(0);
        long maxEnd = results.stream().mapToLong(result -> result.endTime).max().orElse(minStart);
        long spanMs = Math.max(1, maxEnd - minStart);
        return count * 1000.0 / spanMs;
    }

    private static DurationStats calculateDurationStats(List<Long> costs) {
        if (costs == null || costs.isEmpty()) {
            return new DurationStats(0, 0, 0, 0, 0, 0);
        }
        List<Long> sorted = new ArrayList<>(costs);
        Collections.sort(sorted);
        long sum = 0;
        for (Long cost : sorted) {
            sum += cost;
        }
        return new DurationStats(
                sum / sorted.size(),
                sorted.get(0),
                sorted.get(sorted.size() - 1),
                getPercentileFromSorted(sorted, PERCENTILE_90),
                getPercentileFromSorted(sorted, PERCENTILE_95),
                getPercentileFromSorted(sorted, PERCENTILE_99)
        );
    }

    private static long getPercentileFromSorted(List<Long> sortedCosts, double percentile) {
        int size = sortedCosts.size();
        if (size == 0) {
            return 0;
        }
        if (size == 1) {
            return sortedCosts.get(0);
        }
        if (size < MIN_SAMPLE_SIZE_FOR_INTERPOLATION) {
            int index = Math.min((int) Math.ceil(percentile * size) - 1, size - 1);
            return sortedCosts.get(Math.max(0, index));
        }
        double index = percentile * (size - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);
        if (lowerIndex == upperIndex) {
            return sortedCosts.get(lowerIndex);
        }
        long lowerValue = sortedCosts.get(lowerIndex);
        long upperValue = sortedCosts.get(upperIndex);
        double fraction = index - lowerIndex;
        return Math.round(lowerValue + (upperValue - lowerValue) * fraction);
    }

    private interface NamedReportRow {
        String name();
    }

    record HttpReportRow(String name,
                         long total,
                         long success,
                         long fail,
                         double successRate,
                         double qps,
                         long avg,
                         long min,
                         long max,
                         long p90,
                         long p95,
                         long p99) implements NamedReportRow {
    }

    record StreamReportRow(String name,
                           long total,
                           long success,
                           long fail,
                           double successRate,
                           long sentMessages,
                           long receivedMessages,
                           long matchedMessages,
                           double sendRate,
                           double receiveRate,
                           double matchedRate,
                           long avgFirstMessageLatencyMs,
                           long p90FirstMessageLatencyMs,
                           long p95FirstMessageLatencyMs,
                           long p99FirstMessageLatencyMs,
                           long avgDurationMs,
                           long p95DurationMs) implements NamedReportRow {
    }

    private record DurationStats(long avg, long min, long max, long p90, long p95, long p99) {
    }
}

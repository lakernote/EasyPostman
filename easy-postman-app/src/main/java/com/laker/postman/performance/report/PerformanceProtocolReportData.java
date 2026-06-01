package com.laker.postman.performance.report;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceReportSnapshot;
import com.laker.postman.performance.core.model.PerformanceStatsSnapshot;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportApi;
import com.laker.postman.performance.core.report.PerformanceJsonReportBytes;
import com.laker.postman.performance.core.report.PerformanceJsonReportDuration;
import com.laker.postman.performance.core.report.PerformanceJsonReportProtocol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PerformanceProtocolReportData {

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

    public static PerformanceProtocolReportData fromStatsSnapshot(PerformanceStatsSnapshot snapshot, String totalRowName) {
        if (snapshot == null) {
            return new PerformanceProtocolReportData(List.of(), List.of(), List.of());
        }
        return new PerformanceProtocolReportData(
                buildHttpRows(snapshot, totalRowName),
                buildStreamRows(snapshot, PerformanceProtocol.WEBSOCKET, totalRowName),
                buildStreamRows(snapshot, PerformanceProtocol.SSE, totalRowName)
        );
    }

    public static PerformanceProtocolReportData fromReportSnapshot(PerformanceReportSnapshot snapshot, String totalRowName) {
        if (snapshot == null) {
            return new PerformanceProtocolReportData(List.of(), List.of(), List.of());
        }
        return new PerformanceProtocolReportData(
                buildHttpRows(snapshot.completedStats(), totalRowName),
                buildStreamRows(snapshot, PerformanceProtocol.WEBSOCKET, totalRowName),
                buildStreamRows(snapshot, PerformanceProtocol.SSE, totalRowName)
        );
    }

    public static PerformanceProtocolReportData fromJsonReport(PerformanceJsonReport report, String totalRowName) {
        if (report == null || report.getProtocols() == null) {
            return new PerformanceProtocolReportData(List.of(), List.of(), List.of());
        }
        return new PerformanceProtocolReportData(
                buildHttpRows(reportProtocol(report, PerformanceProtocol.HTTP), totalRowName),
                buildStreamRows(reportProtocol(report, PerformanceProtocol.WEBSOCKET), totalRowName),
                buildStreamRows(reportProtocol(report, PerformanceProtocol.SSE), totalRowName)
        );
    }

    public List<HttpReportRow> httpRows() {
        return httpRows;
    }

    public List<StreamReportRow> webSocketRows() {
        return webSocketRows;
    }

    public List<StreamReportRow> sseRows() {
        return sseRows;
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

    private static List<HttpReportRow> buildHttpRows(PerformanceJsonReportProtocol protocolReport,
                                                     String totalRowName) {
        if (protocolReport == null) {
            return List.of();
        }
        List<HttpReportRow> rows = new ArrayList<>();
        for (PerformanceJsonReportApi api : safeApis(protocolReport)) {
            rows.add(toHttpRow(api, null));
        }
        sortByName(rows);
        if (protocolReport.getTotal() != null && protocolReport.getTotal().getTotal() > 0) {
            rows.add(toHttpRow(protocolReport.getTotal(), totalRowName));
        }
        return rows;
    }

    private static List<StreamReportRow> buildStreamRows(PerformanceStatsSnapshot snapshot,
                                                         PerformanceProtocol protocol,
                                                         String totalRowName) {
        List<StreamReportRow> rows = buildStreamRows(snapshot, protocol);
        PerformanceStatsSnapshot.ApiSummary total = snapshot.totalFor(protocol, totalRowName);
        if (total != null && total.total() > 0) {
            rows.add(toStreamRow(total));
        }
        return rows;
    }

    private static List<StreamReportRow> buildStreamRows(PerformanceStatsSnapshot snapshot,
                                                         PerformanceProtocol protocol) {
        List<StreamReportRow> rows = new ArrayList<>();
        for (PerformanceStatsSnapshot.ApiSummary summary : snapshot.summaries()) {
            if (summary.protocol() == protocol) {
                rows.add(toStreamRow(summary));
            }
        }
        sortByName(rows);
        return rows;
    }

    private static List<StreamReportRow> buildStreamRows(PerformanceJsonReportProtocol protocolReport,
                                                         String totalRowName) {
        if (protocolReport == null) {
            return List.of();
        }
        List<StreamReportRow> rows = new ArrayList<>();
        for (PerformanceJsonReportApi api : safeApis(protocolReport)) {
            rows.add(toStreamRow(api, null));
        }
        sortByName(rows);
        if (protocolReport.getTotal() != null && protocolReport.getTotal().getTotal() > 0) {
            rows.add(toStreamRow(protocolReport.getTotal(), totalRowName));
        }
        return rows;
    }

    private static List<StreamReportRow> buildStreamRows(PerformanceReportSnapshot snapshot,
                                                         PerformanceProtocol protocol,
                                                         String totalRowName) {
        PerformanceRealtimeMetrics.LiveProtocolSnapshot liveSnapshot =
                liveProtocolSnapshot(snapshot.liveSnapshot(), protocol);
        if (liveSnapshot == null || !liveSnapshot.hasData()) {
            return buildStreamRows(snapshot.completedStats(), protocol, totalRowName);
        }

        Map<String, MutableStreamReportRow> rowsByName = new LinkedHashMap<>();
        for (StreamReportRow row : buildStreamRows(snapshot.completedStats(), protocol)) {
            rowsByName.computeIfAbsent(row.name(), ignored -> new MutableStreamReportRow(row.name()))
                    .add(row);
        }
        for (StreamReportRow row : buildLiveStreamRows(liveSnapshot, totalRowName)) {
            rowsByName.computeIfAbsent(row.name(), ignored -> new MutableStreamReportRow(row.name()))
                    .add(row);
        }

        List<StreamReportRow> rows = rowsByName.values().stream()
                .map(MutableStreamReportRow::toRow)
                .toList();
        rows = new ArrayList<>(rows);
        sortByName(rows);
        if (!rows.isEmpty() && shouldAddTotalRow(rows, totalRowName)) {
            rows.add(totalStreamRow(totalRowName, rows));
        }
        return rows;
    }

    private static boolean shouldAddTotalRow(List<StreamReportRow> rows, String totalRowName) {
        return rows.size() != 1 || !totalRowName.equals(rows.get(0).name());
    }

    private static PerformanceRealtimeMetrics.LiveProtocolSnapshot liveProtocolSnapshot(
            PerformanceRealtimeMetrics.LiveSnapshot liveSnapshot,
            PerformanceProtocol protocol) {
        if (liveSnapshot == null || protocol == null) {
            return null;
        }
        return switch (protocol) {
            case WEBSOCKET -> liveSnapshot.webSocket();
            case SSE -> liveSnapshot.sse();
            case HTTP -> null;
        };
    }

    private static List<StreamReportRow> buildLiveStreamRows(PerformanceRealtimeMetrics.LiveProtocolSnapshot liveSnapshot,
                                                             String totalRowName) {
        if (liveSnapshot.apiSnapshots() == null || liveSnapshot.apiSnapshots().isEmpty()) {
            return liveSnapshot.hasData()
                    ? List.of(toLiveStreamRow(totalRowName, liveSnapshot))
                    : List.of();
        }
        List<StreamReportRow> rows = new ArrayList<>();
        for (PerformanceRealtimeMetrics.LiveApiSnapshot apiSnapshot : liveSnapshot.apiSnapshots()) {
            rows.add(toLiveStreamRow(resolveLiveApiName(apiSnapshot, totalRowName), apiSnapshot.metrics()));
        }
        return rows;
    }

    private static String resolveLiveApiName(PerformanceRealtimeMetrics.LiveApiSnapshot apiSnapshot,
                                             String totalRowName) {
        if (apiSnapshot == null) {
            return totalRowName;
        }
        String apiName = apiSnapshot.apiName();
        if (apiName != null && !apiName.isBlank()) {
            return apiName;
        }
        String apiId = apiSnapshot.apiId();
        if (apiId != null && !apiId.isBlank()) {
            return apiId;
        }
        return totalRowName;
    }

    private static void sortByName(List<? extends NamedReportRow> rows) {
        rows.sort(Comparator.comparing(NamedReportRow::name, String.CASE_INSENSITIVE_ORDER));
    }

    private static PerformanceJsonReportProtocol reportProtocol(PerformanceJsonReport report,
                                                                PerformanceProtocol protocol) {
        if (report == null || report.getProtocols() == null || protocol == null) {
            return null;
        }
        return report.getProtocols().get(protocol.name());
    }

    private static List<PerformanceJsonReportApi> safeApis(PerformanceJsonReportProtocol protocolReport) {
        return protocolReport == null || protocolReport.getApis() == null
                ? List.of()
                : protocolReport.getApis();
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
                summary.sentBytesPerSecond(),
                summary.receivedBytesPerSecond(),
                summary.avgReceivedBytes(),
                stats.avg(),
                stats.min(),
                stats.max(),
                stats.p90(),
                stats.p95(),
                stats.p99()
        );
    }

    private static HttpReportRow toHttpRow(PerformanceJsonReportApi api, String fallbackName) {
        PerformanceJsonReportDuration duration = api.getDurationMs() == null
                ? PerformanceJsonReportDuration.builder().build()
                : api.getDurationMs();
        PerformanceJsonReportBytes bytes = api.getBytes() == null
                ? PerformanceJsonReportBytes.builder().build()
                : api.getBytes();
        return new HttpReportRow(
                resolveApiName(api, fallbackName),
                api.getTotal(),
                api.getSuccess(),
                api.getFailed(),
                api.getSuccessRate(),
                api.getSamplesPerSecond(),
                bytes.getSentBytesPerSecond(),
                bytes.getReceivedBytesPerSecond(),
                bytes.getAvgReceivedBytes(),
                duration.getAvg(),
                duration.getMin(),
                duration.getMax(),
                duration.getP90(),
                duration.getP95(),
                duration.getP99()
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

    private static StreamReportRow toStreamRow(PerformanceJsonReportApi api, String fallbackName) {
        PerformanceJsonReportDuration duration = api.getDurationMs() == null
                ? PerformanceJsonReportDuration.builder().build()
                : api.getDurationMs();
        PerformanceJsonReportDuration firstLatency = api.getFirstMessageLatencyMs() == null
                ? PerformanceJsonReportDuration.builder().build()
                : api.getFirstMessageLatencyMs();
        return new StreamReportRow(
                resolveApiName(api, fallbackName),
                api.getTotal(),
                api.getSuccess(),
                api.getFailed(),
                api.getSuccessRate(),
                api.getStream().getSentMessages(),
                api.getStream().getReceivedMessages(),
                api.getStream().getMatchedMessages(),
                api.getStream().getSendRate(),
                api.getStream().getReceiveRate(),
                api.getStream().getMatchedRate(),
                firstLatency.getAvg(),
                firstLatency.getP90(),
                firstLatency.getP95(),
                firstLatency.getP99(),
                duration.getAvg(),
                duration.getP95()
        );
    }

    private static String resolveApiName(PerformanceJsonReportApi api, String fallbackName) {
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }
        if (api.getName() != null && !api.getName().isBlank()) {
            return api.getName();
        }
        if (api.getApiId() != null && !api.getApiId().isBlank()) {
            return api.getApiId();
        }
        return api.getProtocol();
    }

    private static StreamReportRow toLiveStreamRow(String name,
                                                   PerformanceRealtimeMetrics.LiveProtocolSnapshot snapshot) {
        PerformanceStatsSnapshot.DurationStats firstLatencyStats = snapshot.firstMessageLatencyStats() == null
                ? PerformanceStatsSnapshot.DurationStats.empty()
                : snapshot.firstMessageLatencyStats();
        PerformanceStatsSnapshot.DurationStats activeDurationStats = snapshot.activeDurationStats() == null
                ? PerformanceStatsSnapshot.DurationStats.empty()
                : snapshot.activeDurationStats();
        int activeSessions = Math.max(0, snapshot.activeSessions());
        return new StreamReportRow(
                name,
                activeSessions,
                activeSessions,
                0,
                successRate(activeSessions, activeSessions),
                snapshot.sentMessages(),
                snapshot.receivedMessages(),
                snapshot.matchedMessages(),
                snapshot.sendRate(),
                snapshot.receiveRate(),
                snapshot.matchedRate(),
                snapshot.avgFirstMessageLatencyMs(),
                firstLatencyStats.p90(),
                firstLatencyStats.p95(),
                firstLatencyStats.p99(),
                activeDurationStats.avg(),
                activeDurationStats.p95()
        );
    }

    private static StreamReportRow totalStreamRow(String totalRowName, List<StreamReportRow> rows) {
        MutableStreamReportRow total = new MutableStreamReportRow(totalRowName);
        for (StreamReportRow row : rows) {
            total.add(row);
        }
        return total.toRow();
    }

    private static double successRate(long total, long success) {
        return total > 0 ? success * 100.0 / total : 0;
    }

    public sealed interface NamedReportRow permits HttpReportRow, StreamReportRow {
        String name();
    }

    public record HttpReportRow(String name,
                                long total,
                                long success,
                                long fail,
                                double successRate,
                                // QPS：请求数 / 样本窗口，GUI 只负责格式化展示，不重新计算。
                                double qps,
                                // 发送字节速率：请求头 + 请求体，单位 bytes/s，展示层换算为 KB/s。
                                double sentBytesPerSecond,
                                // 接收字节速率：响应头 + 响应体，单位 bytes/s，展示层换算为 KB/s。
                                double receivedBytesPerSecond,
                                // 平均接收字节数：receivedBytes / total，对齐 JMeter Avg. Bytes 口径。
                                long avgReceivedBytes,
                                // 请求耗时分布：只包含 sampler/request 耗时，不包含脚本、断言和 UI 刷新。
                                long avg,
                                long min,
                                long max,
                                long p90,
                                long p95,
                                long p99) implements NamedReportRow {
    }

    public record StreamReportRow(String name,
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

    private static final class MutableStreamReportRow {
        private final String name;
        private long total;
        private long success;
        private long fail;
        private long sentMessages;
        private long receivedMessages;
        private long matchedMessages;
        private double sendRate;
        private double receiveRate;
        private double matchedRate;
        private long avgFirstMessageLatencyMs;
        private long p90FirstMessageLatencyMs;
        private long p95FirstMessageLatencyMs;
        private long p99FirstMessageLatencyMs;
        private long avgDurationMs;
        private long p95DurationMs;

        private MutableStreamReportRow(String name) {
            this.name = name;
        }

        private void add(StreamReportRow row) {
            long previousTotal = total;
            total += row.total();
            success += row.success();
            fail += row.fail();
            sentMessages += row.sentMessages();
            receivedMessages += row.receivedMessages();
            matchedMessages += row.matchedMessages();
            sendRate += row.sendRate();
            receiveRate += row.receiveRate();
            matchedRate += row.matchedRate();
            avgFirstMessageLatencyMs = weightedAverage(
                    avgFirstMessageLatencyMs,
                    positiveMetricWeight(avgFirstMessageLatencyMs, previousTotal),
                    row.avgFirstMessageLatencyMs(),
                    positiveMetricWeight(row.avgFirstMessageLatencyMs(), row.total())
            );
            p90FirstMessageLatencyMs = Math.max(p90FirstMessageLatencyMs, row.p90FirstMessageLatencyMs());
            p95FirstMessageLatencyMs = Math.max(p95FirstMessageLatencyMs, row.p95FirstMessageLatencyMs());
            p99FirstMessageLatencyMs = Math.max(p99FirstMessageLatencyMs, row.p99FirstMessageLatencyMs());
            avgDurationMs = weightedAverage(
                    avgDurationMs,
                    positiveMetricWeight(avgDurationMs, previousTotal),
                    row.avgDurationMs(),
                    positiveMetricWeight(row.avgDurationMs(), row.total())
            );
            p95DurationMs = Math.max(p95DurationMs, row.p95DurationMs());
        }

        private StreamReportRow toRow() {
            return new StreamReportRow(
                    name,
                    total,
                    success,
                    fail,
                    successRate(total, success),
                    sentMessages,
                    receivedMessages,
                    matchedMessages,
                    sendRate,
                    receiveRate,
                    matchedRate,
                    avgFirstMessageLatencyMs,
                    p90FirstMessageLatencyMs,
                    p95FirstMessageLatencyMs,
                    p99FirstMessageLatencyMs,
                    avgDurationMs,
                    p95DurationMs
            );
        }

        private static long positiveMetricWeight(long value, long weight) {
            return value > 0 ? Math.max(0, weight) : 0;
        }

        private static long weightedAverage(long leftValue, long leftWeight, long rightValue, long rightWeight) {
            long totalWeight = leftWeight + rightWeight;
            if (totalWeight <= 0) {
                return 0;
            }
            return (leftValue * leftWeight + rightValue * rightWeight) / totalWeight;
        }
    }

}

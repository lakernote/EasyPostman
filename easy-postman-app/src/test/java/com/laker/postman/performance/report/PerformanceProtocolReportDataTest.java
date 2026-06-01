package com.laker.postman.performance.report;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceReportSnapshot;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.RequestResult;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportApi;
import com.laker.postman.performance.core.report.PerformanceJsonReportBytes;
import com.laker.postman.performance.core.report.PerformanceJsonReportDuration;
import com.laker.postman.performance.core.report.PerformanceJsonReportProtocol;
import com.laker.postman.performance.core.report.PerformanceJsonReportStream;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceProtocolReportDataTest {

    @Test
    public void shouldBuildSeparateProtocolRows() {
        RequestResult http = new RequestResult(1_000, 1_100, true, "http-api", "HTTP API", PerformanceProtocol.HTTP);
        http.sentBytes = 128;
        http.receivedBytes = 512;
        RequestResult ws = new RequestResult(2_000, 2_500, true, "ws-api", "WS API", PerformanceProtocol.WEBSOCKET);
        ws.sentMessages = 2;
        ws.receivedMessages = 5;
        ws.matchedMessages = 3;
        ws.firstMessageLatencyMs = 90;
        RequestResult sse = new RequestResult(3_000, 4_000, false, "sse-api", "SSE API", PerformanceProtocol.SSE);
        sse.receivedMessages = 7;
        sse.matchedMessages = 4;
        sse.firstMessageLatencyMs = 140;

        PerformanceProtocolReportData reportData = PerformanceProtocolReportData.fromStatsSnapshot(
                statsSnapshot(http, ws, sse),
                "Total"
        );

        assertEquals(reportData.httpRows().get(0).name(), "HTTP API");
        assertEquals(reportData.httpRows().get(0).qps(), 10.0);
        assertEquals(reportData.httpRows().get(0).sentBytesPerSecond(), 1280.0);
        assertEquals(reportData.httpRows().get(0).receivedBytesPerSecond(), 5120.0);
        assertEquals(reportData.httpRows().get(0).avgReceivedBytes(), 512L);
        assertEquals(reportData.webSocketRows().get(0).name(), "WS API");
        assertEquals(reportData.webSocketRows().get(0).sentMessages(), 2);
        assertEquals(reportData.webSocketRows().get(0).receivedMessages(), 5);
        assertEquals(reportData.sseRows().get(0).name(), "SSE API");
        assertEquals(reportData.sseRows().get(0).receivedMessages(), 7);
        assertEquals(reportData.sseRows().get(0).matchedMessages(), 4);
    }

    @Test
    public void shouldBuildRowsFromAggregatedStatsSnapshot() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        collector.record(new RequestResult(1_000, 1_100, true, "http-api", "HTTP API", PerformanceProtocol.HTTP));
        collector.record(new RequestResult(1_100, 1_300, false, "http-api", "HTTP API", PerformanceProtocol.HTTP));

        PerformanceProtocolReportData reportData = PerformanceProtocolReportData.fromStatsSnapshot(
                collector.snapshot(),
                "Total"
        );

        assertEquals(reportData.httpRows().get(0).name(), "HTTP API");
        assertEquals(reportData.httpRows().get(0).total(), 2L);
        assertEquals(reportData.httpRows().get(0).success(), 1L);
        assertEquals(reportData.httpRows().get(0).fail(), 1L);
        assertEquals(reportData.httpRows().get(1).name(), "Total");
        assertEquals(reportData.httpRows().get(1).total(), 2L);
    }

    @Test
    public void shouldMergeLiveStreamRowsIntoRealtimeReports() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        RequestResult completed = new RequestResult(1_000, 1_500, true, "ws-api", "WS API", PerformanceProtocol.WEBSOCKET);
        completed.sentMessages = 1;
        completed.receivedMessages = 2;
        completed.matchedMessages = 1;
        completed.firstMessageLatencyMs = 90;
        collector.record(completed);

        PerformanceRealtimeMetrics realtimeMetrics = new PerformanceRealtimeMetrics();
        Object activeSession = new Object();
        realtimeMetrics.reset(0);
        realtimeMetrics.recordWebSocketSessionStart(activeSession, 2_000, "ws-api", "WS API");
        realtimeMetrics.recordWebSocketSent(activeSession);
        realtimeMetrics.recordWebSocketSent(activeSession);
        realtimeMetrics.recordWebSocketReceived(activeSession);
        realtimeMetrics.recordWebSocketMatched(activeSession);
        realtimeMetrics.recordWebSocketFirstMessageLatency(activeSession, 120);

        PerformanceProtocolReportData reportData = PerformanceProtocolReportData.fromReportSnapshot(
                PerformanceReportSnapshot.of(collector.snapshot(), realtimeMetrics.liveSnapshot(4_000)),
                "Total"
        );

        assertEquals(reportData.webSocketRows().size(), 2);

        PerformanceProtocolReportData.StreamReportRow apiRow = reportData.webSocketRows().get(0);
        assertEquals(apiRow.name(), "WS API");
        assertEquals(apiRow.total(), 2L);
        assertEquals(apiRow.success(), 2L);
        assertEquals(apiRow.sentMessages(), 3L);
        assertEquals(apiRow.receivedMessages(), 3L);
        assertEquals(apiRow.matchedMessages(), 2L);
        assertEquals(apiRow.avgFirstMessageLatencyMs(), 105L);
        assertEquals(apiRow.avgDurationMs(), 1250L);

        PerformanceProtocolReportData.StreamReportRow totalRow = reportData.webSocketRows().get(1);
        assertEquals(totalRow.name(), "Total");
        assertEquals(totalRow.total(), 2L);
        assertEquals(totalRow.success(), 2L);
        assertEquals(totalRow.sentMessages(), 3L);
        assertEquals(totalRow.avgDurationMs(), 1250L);
    }

    @Test
    public void shouldCalculateSseFirstEventLatencyPercentilesFromStatsSnapshot() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        for (int i = 1; i <= 10; i++) {
            RequestResult result = new RequestResult(i * 1_000L, i * 1_000L + 2_000L,
                    true, "stream", "Stream API", PerformanceProtocol.SSE);
            result.firstMessageLatencyMs = i * 100L;
            result.receivedMessages = 1;
            result.matchedMessages = 1;
            collector.record(result);
        }

        PerformanceProtocolReportData reportData = PerformanceProtocolReportData.fromStatsSnapshot(collector.snapshot(), "Total");
        PerformanceProtocolReportData.StreamReportRow row = reportData.sseRows().get(0);

        assertEquals(row.name(), "Stream API");
        assertEquals(row.avgFirstMessageLatencyMs(), 550L);
        assertEquals(row.p90FirstMessageLatencyMs(), 900L);
        assertEquals(row.p95FirstMessageLatencyMs(), 1000L);
        assertEquals(row.p99FirstMessageLatencyMs(), 1000L);
    }

    @Test
    public void shouldBuildRowsFromDistributedJsonReport() {
        PerformanceJsonReport report = PerformanceJsonReport.builder()
                .protocols(Map.of(
                        "HTTP",
                        PerformanceJsonReportProtocol.builder()
                                .protocol("HTTP")
                                .total(PerformanceJsonReportApi.builder()
                                        .protocol("HTTP")
                                        .total(5L)
                                        .success(4L)
                                        .samplesPerSecond(12.5)
                                        .bytes(PerformanceJsonReportBytes.builder()
                                                .sentBytesPerSecond(512.0)
                                                .receivedBytesPerSecond(1024.0)
                                                .avgReceivedBytes(256L)
                                                .build())
                                        .durationMs(PerformanceJsonReportDuration.builder()
                                                .avg(30L)
                                                .min(10L)
                                                .max(90L)
                                                .p90(70L)
                                                .p95(80L)
                                                .p99(90L)
                                                .build())
                                        .build())
                                .apis(List.of(PerformanceJsonReportApi.builder()
                                        .apiId("api-1")
                                        .name("Remote API")
                                        .protocol("HTTP")
                                        .total(5L)
                                        .success(4L)
                                        .samplesPerSecond(12.5)
                                        .bytes(PerformanceJsonReportBytes.builder()
                                                .sentBytesPerSecond(512.0)
                                                .receivedBytesPerSecond(1024.0)
                                                .avgReceivedBytes(256L)
                                                .build())
                                        .durationMs(PerformanceJsonReportDuration.builder()
                                                .avg(30L)
                                                .min(10L)
                                                .max(90L)
                                                .p90(70L)
                                                .p95(80L)
                                                .p99(90L)
                                                .build())
                                        .build()))
                                .build(),
                        "WEBSOCKET",
                        PerformanceJsonReportProtocol.builder()
                                .protocol("WEBSOCKET")
                                .total(PerformanceJsonReportApi.builder()
                                        .protocol("WEBSOCKET")
                                        .total(2L)
                                        .success(2L)
                                        .stream(PerformanceJsonReportStream.builder()
                                                .sentMessages(3L)
                                                .receivedMessages(4L)
                                                .matchedMessages(1L)
                                                .sendRate(1.5)
                                                .receiveRate(2.5)
                                                .matchedRate(0.5)
                                                .build())
                                        .firstMessageLatencyMs(PerformanceJsonReportDuration.builder()
                                                .avg(45L)
                                                .p90(60L)
                                                .p95(70L)
                                                .p99(80L)
                                                .build())
                                        .durationMs(PerformanceJsonReportDuration.builder()
                                                .avg(500L)
                                                .p95(900L)
                                                .build())
                                        .build())
                                .apis(List.of())
                                .build()
                ))
                .build();

        PerformanceProtocolReportData reportData = PerformanceProtocolReportData.fromJsonReport(report, "Total");

        assertEquals(reportData.httpRows().get(0).name(), "Remote API");
        assertEquals(reportData.httpRows().get(0).total(), 5L);
        assertEquals(reportData.httpRows().get(0).sentBytesPerSecond(), 512.0);
        assertEquals(reportData.httpRows().get(0).receivedBytesPerSecond(), 1024.0);
        assertEquals(reportData.httpRows().get(0).avgReceivedBytes(), 256L);
        assertEquals(reportData.httpRows().get(1).name(), "Total");
        assertEquals(reportData.httpRows().get(1).qps(), 12.5);
        assertEquals(reportData.webSocketRows().get(0).name(), "Total");
        assertEquals(reportData.webSocketRows().get(0).sentMessages(), 3L);
        assertEquals(reportData.webSocketRows().get(0).avgFirstMessageLatencyMs(), 45L);
        assertEquals(reportData.webSocketRows().get(0).avgDurationMs(), 500L);
    }

    private static com.laker.postman.performance.core.model.PerformanceStatsSnapshot statsSnapshot(
            RequestResult... results) {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        for (RequestResult result : results) {
            collector.record(result);
        }
        return collector.snapshot();
    }
}

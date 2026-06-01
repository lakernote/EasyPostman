package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class PerformanceJsonReportTrendWindowSamplerTest {

    @Test
    public void shouldCalculateQpsFromRemoteRequestDeltas() {
        PerformanceJsonReportTrendWindowSampler sampler = new PerformanceJsonReportTrendWindowSampler();
        sampler.reset(1_000L);

        PerformanceTrendSnapshot first = sampler.drainReportDelta(
                70,
                0,
                0,
                100,
                5,
                report(100, 95, 5, 4),
                2_000L
        );
        PerformanceTrendSnapshot second = sampler.drainReportDelta(
                70,
                0,
                0,
                150,
                8,
                report(150, 142, 8, 5),
                3_000L
        );

        assertEquals(first.activeUsers(), 70);
        assertEquals(first.http().samples(), 100);
        assertEquals(first.http().sampleRate(), 100.0);
        assertEquals(second.http().samples(), 50);
        assertEquals(second.http().failures(), 3);
        assertEquals(second.http().sampleRate(), 50.0);
        assertEquals(second.http().avgDurationMs(), 5.0);
    }

    @Test
    public void shouldCalculateWebSocketAndSseMetricsFromRemoteReportDeltas() {
        PerformanceJsonReportTrendWindowSampler sampler = new PerformanceJsonReportTrendWindowSampler();
        sampler.reset(1_000L);

        PerformanceTrendSnapshot snapshot = sampler.drainReportDelta(
                10,
                8,
                7,
                15,
                1,
                streamReport(),
                2_000L
        );

        assertEquals(snapshot.activeWebSocketConnections(), 8);
        assertEquals(snapshot.activeSseStreams(), 7);
        assertEquals(snapshot.webSocket().samples(), 8);
        assertEquals(snapshot.webSocket().sentMessages(), 100);
        assertEquals(snapshot.webSocket().receivedMessages(), 80);
        assertEquals(snapshot.webSocket().sentRate(), 100.0);
        assertEquals(snapshot.webSocket().receivedRate(), 80.0);
        assertEquals(snapshot.sse().samples(), 7);
        assertEquals(snapshot.sse().receivedMessages(), 35);
        assertEquals(snapshot.sse().receivedRate(), 35.0);
        assertEquals(snapshot.overview().sentMessages(), 100);
        assertEquals(snapshot.overview().receivedMessages(), 115);
    }

    @Test
    public void shouldNotCountLiveStreamSessionsAsCompletedSamples() {
        PerformanceJsonReportTrendWindowSampler sampler = new PerformanceJsonReportTrendWindowSampler();
        sampler.reset(1_000L);

        sampler.drainReportDelta(
                10,
                10,
                0,
                0,
                0,
                liveWebSocketReport(10, 200),
                2_000L
        );
        PerformanceTrendSnapshot finalSnapshot = sampler.drainReportDelta(
                0,
                0,
                0,
                10,
                10,
                completedWebSocketReport(10, 0, 10, 260),
                3_000L
        );

        assertEquals(finalSnapshot.webSocket().samples(), 10);
        assertEquals(finalSnapshot.webSocket().failures(), 10);
        assertEquals(finalSnapshot.webSocket().failurePercent(), 100.0);
        assertEquals(finalSnapshot.webSocket().sentMessages(), 60);
    }

    @Test
    public void shouldUsePreviousActiveUsersWhenTerminalPollContainsRequestDeltas() {
        PerformanceJsonReportTrendWindowSampler sampler = new PerformanceJsonReportTrendWindowSampler();
        sampler.reset(1_000L);

        sampler.drainReportDelta(
                2,
                0,
                0,
                0,
                0,
                report(0, 0, 0, 0),
                2_000L
        );
        PerformanceTrendSnapshot terminalSnapshot = sampler.drainReportDelta(
                0,
                0,
                0,
                12,
                0,
                report(12, 12, 0, 200),
                3_000L
        );

        assertEquals(terminalSnapshot.activeUsers(), 2);
        assertEquals(terminalSnapshot.http().samples(), 12);
        assertEquals(terminalSnapshot.http().sampleRate(), 12.0);
    }

    private static PerformanceJsonReport report(long total, long success, long failed, long avgDuration) {
        PerformanceJsonReportApi httpTotal = PerformanceJsonReportApi.builder()
                .name("HTTP Total")
                .protocol("HTTP")
                .total(total)
                .success(success)
                .failed(failed)
                .durationMs(PerformanceJsonReportDuration.builder().avg(avgDuration).build())
                .build();
        return PerformanceJsonReport.builder()
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(total)
                        .successRequests(success)
                        .failedRequests(failed)
                        .build())
                .protocols(Map.of("HTTP", PerformanceJsonReportProtocol.builder()
                        .protocol("HTTP")
                        .total(httpTotal)
                        .build()))
                .build();
    }

    private static PerformanceJsonReport streamReport() {
        PerformanceJsonReportApi webSocketTotal = PerformanceJsonReportApi.builder()
                .name("WebSocket Total")
                .protocol("WEBSOCKET")
                .total(8L)
                .success(8L)
                .failed(0L)
                .stream(PerformanceJsonReportStream.builder()
                        .sentMessages(100L)
                        .receivedMessages(80L)
                        .build())
                .build();
        PerformanceJsonReportApi sseTotal = PerformanceJsonReportApi.builder()
                .name("SSE Total")
                .protocol("SSE")
                .total(7L)
                .success(6L)
                .failed(1L)
                .stream(PerformanceJsonReportStream.builder()
                        .receivedMessages(35L)
                        .matchedMessages(12L)
                        .build())
                .build();
        return PerformanceJsonReport.builder()
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(15L)
                        .successRequests(14L)
                        .failedRequests(1L)
                        .build())
                .protocols(Map.of(
                        "WEBSOCKET", PerformanceJsonReportProtocol.builder()
                                .protocol("WEBSOCKET")
                                .total(webSocketTotal)
                                .build(),
                        "SSE", PerformanceJsonReportProtocol.builder()
                                .protocol("SSE")
                                .total(sseTotal)
                                .build()
                ))
                .build();
    }

    private static PerformanceJsonReport liveWebSocketReport(long activeSessions, long sentMessages) {
        PerformanceJsonReportApi webSocketTotal = PerformanceJsonReportApi.builder()
                .name("WebSocket Total")
                .protocol("WEBSOCKET")
                .total(activeSessions)
                .success(activeSessions)
                .failed(0L)
                .stream(PerformanceJsonReportStream.builder()
                        .sentMessages(sentMessages)
                        .receivedMessages(sentMessages)
                        .build())
                .build();
        return PerformanceJsonReport.builder()
                .summary(PerformanceJsonReportSummary.builder().build())
                .protocols(Map.of("WEBSOCKET", PerformanceJsonReportProtocol.builder()
                        .protocol("WEBSOCKET")
                        .total(webSocketTotal)
                        .build()))
                .build();
    }

    private static PerformanceJsonReport completedWebSocketReport(long total,
                                                                  long success,
                                                                  long failed,
                                                                  long sentMessages) {
        PerformanceJsonReportApi webSocketTotal = PerformanceJsonReportApi.builder()
                .name("WebSocket Total")
                .protocol("WEBSOCKET")
                .total(total)
                .success(success)
                .failed(failed)
                .stream(PerformanceJsonReportStream.builder()
                        .sentMessages(sentMessages)
                        .receivedMessages(sentMessages)
                        .build())
                .build();
        return PerformanceJsonReport.builder()
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(total)
                        .successRequests(success)
                        .failedRequests(failed)
                        .build())
                .protocols(Map.of("WEBSOCKET", PerformanceJsonReportProtocol.builder()
                        .protocol("WEBSOCKET")
                        .total(webSocketTotal)
                        .build()))
                .build();
    }
}

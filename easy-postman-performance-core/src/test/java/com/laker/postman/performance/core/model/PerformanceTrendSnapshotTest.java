package com.laker.postman.performance.core.model;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceTrendSnapshotTest {

    @Test
    public void terminalIdleSnapshotShouldOnlyPublishActiveCounts() {
        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.terminalIdle();

        assertEquals(snapshot.activeUsers(), 0);
        assertEquals(snapshot.activeWebSocketConnections(), 0);
        assertEquals(snapshot.activeSseStreams(), 0);
        assertTrue(Double.isNaN(snapshot.http().sampleRate()));
        assertTrue(Double.isNaN(snapshot.http().failurePercent()));
        assertTrue(Double.isNaN(snapshot.webSocket().sentRate()));
        assertTrue(Double.isNaN(snapshot.sse().receivedRate()));
    }

    @Test
    public void shouldSeparateWindowMetricsByProtocol() {
        RequestResult http = new RequestResult(1_000, 1_100, true, "http-api", PerformanceProtocol.HTTP);
        RequestResult ws = new RequestResult(1_200, 1_700, true, "ws-api", PerformanceProtocol.WEBSOCKET);
        ws.sentMessages = 2;
        ws.receivedMessages = 6;
        ws.firstMessageLatencyMs = 80;
        RequestResult sse = new RequestResult(1_300, 2_300, false, "sse-api", PerformanceProtocol.SSE);
        sse.receivedMessages = 5;
        sse.matchedMessages = 4;
        sse.firstMessageLatencyMs = 150;

        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                List.of(http, ws, sse),
                1_000,
                2_500,
                12,
                3,
                4,
                1_000
        );

        assertEquals(snapshot.activeUsers(), 12);
        assertEquals(snapshot.activeWebSocketConnections(), 3);
        assertEquals(snapshot.activeSseStreams(), 4);
        assertEquals(snapshot.http().samples(), 1);
        assertEquals(snapshot.webSocket().receivedMessages(), 6);
        assertEquals(snapshot.webSocket().receivedRate(), 12.0);
        assertEquals(snapshot.webSocket().avgFirstMessageLatencyMs(), 80.0);
        assertEquals(snapshot.sse().failurePercent(), 100.0);
        assertEquals(snapshot.sse().receivedMessages(), 5);
        assertEquals(snapshot.sse().matchedMessages(), 4);
    }

    @Test
    public void shouldUseFixedStepIntervalForRequestWindowRates() {
        RequestResult first = new RequestResult(1_000, 1_010, true, "http-api", PerformanceProtocol.HTTP);
        RequestResult second = new RequestResult(1_020, 1_030, true, "http-api", PerformanceProtocol.HTTP);
        RequestResult ws = new RequestResult(1_000, 3_000, true, "ws-api", PerformanceProtocol.WEBSOCKET);
        ws.sentMessages = 10;
        ws.receivedMessages = 20;

        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                List.of(first, second, ws),
                1_000,
                3_000,
                3,
                1,
                0,
                1_000
        );

        assertEquals(snapshot.http().sampleRate(), 2.0);
        assertEquals(snapshot.webSocket().sentRate(), 5.0);
        assertEquals(snapshot.webSocket().receivedRate(), 10.0);
    }

    @Test
    public void shouldNotSpikeStreamRatesWhenSessionsCompleteTogether() {
        RequestResult first = new RequestResult(0, 10_000, true, "ws-api", PerformanceProtocol.WEBSOCKET);
        first.sentMessages = 10;
        first.receivedMessages = 10;
        RequestResult second = new RequestResult(0, 10_000, true, "ws-api", PerformanceProtocol.WEBSOCKET);
        second.sentMessages = 10;
        second.receivedMessages = 10;
        RequestResult third = new RequestResult(0, 10_001, true, "ws-api", PerformanceProtocol.WEBSOCKET);
        third.sentMessages = 10;
        third.receivedMessages = 10;

        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                List.of(first, second, third),
                9_000,
                11_000,
                3,
                0,
                0,
                1_000
        );

        assertEquals(snapshot.webSocket().sentRate(), 3.0);
        assertEquals(snapshot.webSocket().receivedRate(), 3.0);
    }

    @Test
    public void shouldNotSpikeSseEventRatesWhenStreamsCompleteTogether() {
        RequestResult first = new RequestResult(0, 10_000, true, "sse-api", PerformanceProtocol.SSE);
        first.receivedMessages = 10;
        first.matchedMessages = 5;
        RequestResult second = new RequestResult(0, 10_000, true, "sse-api", PerformanceProtocol.SSE);
        second.receivedMessages = 10;
        second.matchedMessages = 5;
        RequestResult third = new RequestResult(0, 10_001, true, "sse-api", PerformanceProtocol.SSE);
        third.receivedMessages = 10;
        third.matchedMessages = 5;

        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                List.of(first, second, third),
                9_000,
                11_000,
                3,
                0,
                0,
                1_000
        );

        assertEquals(snapshot.sse().receivedRate(), 3.0);
        assertEquals(snapshot.sse().matchedRate(), 1.5);
    }

    @Test
    public void shouldUseRealtimeStreamRatesBeforeSessionsComplete() {
        PerformanceRealtimeMetrics.Sample realtime = new PerformanceRealtimeMetrics.Sample(
                3.0,
                2.0,
                0.0,
                120.0,
                3,
                4_000.0,
                4.0,
                1.0,
                80.0,
                2,
                5_000.0
        );

        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                List.of(),
                1_000,
                2_000,
                3,
                3,
                2,
                1_000,
                realtime
        );

        assertEquals(snapshot.webSocket().sentRate(), 3.0);
        assertEquals(snapshot.webSocket().receivedRate(), 2.0);
        assertEquals(snapshot.webSocket().avgFirstMessageLatencyMs(), 120.0);
        assertEquals(snapshot.webSocket().avgDurationMs(), 4_000.0);
        assertEquals(snapshot.sse().receivedRate(), 4.0);
        assertEquals(snapshot.sse().matchedRate(), 1.0);
        assertEquals(snapshot.sse().avgFirstMessageLatencyMs(), 80.0);
        assertEquals(snapshot.sse().avgDurationMs(), 5_000.0);
    }
}

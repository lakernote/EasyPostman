package com.laker.postman.panel.performance.model;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceTrendSnapshotTest {

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
        assertEquals(snapshot.webSocket().receivedRate(), 6.0);
        assertEquals(snapshot.webSocket().avgFirstMessageLatencyMs(), 80.0);
        assertEquals(snapshot.sse().failurePercent(), 100.0);
        assertEquals(snapshot.sse().receivedMessages(), 5);
        assertEquals(snapshot.sse().matchedMessages(), 4);
    }

    @Test
    public void shouldUseActualResultSpanForPerSecondRatesLikeLegacyTrend() {
        RequestResult first = new RequestResult(1_000, 1_010, true, "http-api", PerformanceProtocol.HTTP);
        RequestResult second = new RequestResult(1_020, 1_030, true, "http-api", PerformanceProtocol.HTTP);
        RequestResult ws = new RequestResult(1_040, 1_050, true, "ws-api", PerformanceProtocol.WEBSOCKET);
        ws.sentMessages = 4;
        ws.receivedMessages = 6;

        PerformanceTrendSnapshot snapshot = PerformanceTrendSnapshot.fromResults(
                List.of(first, second, ws),
                1_000,
                2_000,
                3,
                1,
                0,
                1_000
        );

        assertEquals(snapshot.http().sampleRate(), 100.0);
        assertEquals(snapshot.webSocket().sentRate(), 4.0);
        assertEquals(snapshot.webSocket().receivedRate(), 6.0);
    }
}

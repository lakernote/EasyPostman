package com.laker.postman.performance.core.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PerformanceCoreStatisticsTest {

    @Test
    public void shouldAggregateProtocolStatsInsideCoreModule() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        collector.record(new RequestResult(1_000, 1_100, true, "http-api", "HTTP API", PerformanceProtocol.HTTP));
        collector.record(new RequestResult(1_200, 1_600, false, "ws-api", "WS API", PerformanceProtocol.WEBSOCKET));

        PerformanceStatsSnapshot snapshot = collector.snapshot();

        assertEquals(snapshot.totalRequests(), 2);
        assertEquals(snapshot.successRequests(), 1);
        assertEquals(snapshot.totalFor(PerformanceProtocol.HTTP, "HTTP Total").total(), 1);
        assertEquals(snapshot.totalFor(PerformanceProtocol.WEBSOCKET, "WS Total").fail(), 1);
    }

    @Test
    public void protocolShouldExposeOnlyStableCoreValuesAndMessageKeys() {
        assertEquals(PerformanceProtocol.HTTP.name(), "HTTP");
        assertEquals(PerformanceProtocol.WEBSOCKET.name(), "WEBSOCKET");
        assertEquals(PerformanceProtocol.SSE.name(), "SSE");
        assertEquals(PerformanceProtocol.HTTP.getMessageKey(), "performance.protocol.http");
    }
}

package com.laker.postman.panel.performance.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PerformanceRealtimeMetricsTest {

    @Test
    public void shouldCalculateWebSocketRatesFromSamplingDeltas() {
        PerformanceRealtimeMetrics metrics = new PerformanceRealtimeMetrics();
        metrics.reset(0);

        metrics.recordWebSocketSent();
        metrics.recordWebSocketSent();
        metrics.recordWebSocketSent();
        metrics.recordWebSocketReceived();
        metrics.recordWebSocketFirstMessageLatency(120);

        PerformanceRealtimeMetrics.Sample first = metrics.sample(1_000);

        assertEquals(first.webSocketSentRate(), 3.0);
        assertEquals(first.webSocketReceivedRate(), 1.0);
        assertEquals(first.webSocketFirstMessageLatencyMs(), 120.0);

        PerformanceRealtimeMetrics.Sample second = metrics.sample(2_000);

        assertEquals(second.webSocketSentRate(), 0.0);
        assertEquals(second.webSocketReceivedRate(), 0.0);
        assertEquals(second.webSocketFirstMessageLatencyMs(), 0.0);
    }

    @Test
    public void shouldCalculateSseRatesFromSamplingDeltas() {
        PerformanceRealtimeMetrics metrics = new PerformanceRealtimeMetrics();
        metrics.reset(0);

        metrics.recordSseReceived();
        metrics.recordSseReceived();
        metrics.recordSseMatched();
        metrics.recordSseFirstMessageLatency(80);
        metrics.recordSseFirstMessageLatency(120);

        PerformanceRealtimeMetrics.Sample sample = metrics.sample(1_000);

        assertEquals(sample.sseReceivedRate(), 2.0);
        assertEquals(sample.sseMatchedRate(), 1.0);
        assertEquals(sample.sseFirstMessageLatencyMs(), 100.0);
    }

    @Test
    public void shouldCalculateActiveStreamDurationsAtSamplingTime() {
        PerformanceRealtimeMetrics metrics = new PerformanceRealtimeMetrics();
        Object firstWebSocket = new Object();
        Object secondWebSocket = new Object();
        Object sseStream = new Object();

        metrics.reset(0);
        metrics.recordWebSocketSessionStart(firstWebSocket, 1_000);
        metrics.recordWebSocketSessionStart(secondWebSocket, 3_000);
        metrics.recordSseSessionStart(sseStream, 2_000);

        PerformanceRealtimeMetrics.Sample sample = metrics.sample(5_000);

        assertEquals(sample.webSocketActiveSessionDurationMs(), 3_000.0);
        assertEquals(sample.sseActiveSessionDurationMs(), 3_000.0);

        metrics.recordWebSocketSessionEnd(firstWebSocket);
        metrics.recordWebSocketSessionEnd(secondWebSocket);
        metrics.recordSseSessionEnd(sseStream);

        PerformanceRealtimeMetrics.Sample emptySample = metrics.sample(6_000);

        assertEquals(emptySample.webSocketActiveSessionDurationMs(), 0.0);
        assertEquals(emptySample.sseActiveSessionDurationMs(), 0.0);
    }
}

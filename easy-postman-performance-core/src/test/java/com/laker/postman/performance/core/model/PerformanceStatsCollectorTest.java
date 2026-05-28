package com.laker.postman.performance.core.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PerformanceStatsCollectorTest {

    @Test
    public void shouldAggregateLargeRunsWithoutKeepingPerRequestResults() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();

        for (int i = 0; i < 100_000; i++) {
            collector.record(new RequestResult(i, i + 100, true, "search", "Search API", PerformanceProtocol.HTTP));
        }

        PerformanceStatsSnapshot snapshot = collector.snapshot();

        assertEquals(snapshot.totalRequests(), 100_000L);
        assertEquals(snapshot.successRequests(), 100_000L);
        assertEquals(snapshot.retainedRequestResultCount(), 0);
        assertEquals(snapshot.summaries().size(), 1);
        assertEquals(snapshot.summaries().get(0).name(), "Search API");
        assertEquals(snapshot.summaries().get(0).durationStats().p95(), 100L);
    }

    @Test
    public void statsCollectorShouldOnlyExposeReportAggregationApi() {
        assertFalse(hasMethodNamed(PerformanceStatsCollector.class, "setTrendEnabled"));
        assertFalse(hasMethodNamed(PerformanceStatsCollector.class, "sampleTrendSnapshot"));
    }

    @Test
    public void shouldAggregateSseFirstEventLatencyPercentiles() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();

        for (int i = 1; i <= 10; i++) {
            RequestResult result = new RequestResult(i * 1_000L, i * 1_000L + 2_000L,
                    true, "stream", "Stream API", PerformanceProtocol.SSE);
            result.firstMessageLatencyMs = i * 100L;
            collector.record(result);
        }

        PerformanceStatsSnapshot.ApiSummary summary = collector.snapshot().summaries().get(0);

        assertEquals(summary.avgFirstMessageLatencyMs(), 550L);
        assertEquals(summary.firstMessageLatencyStats().avg(), 550L);
        assertEquals(summary.firstMessageLatencyStats().p90(), 900L);
        assertEquals(summary.firstMessageLatencyStats().p95(), 1000L);
        assertEquals(summary.firstMessageLatencyStats().p99(), 1000L);
    }

    private static boolean hasMethodNamed(Class<?> type, String methodName) {
        return java.util.Arrays.stream(type.getMethods())
                .anyMatch(method -> methodName.equals(method.getName()));
    }
}

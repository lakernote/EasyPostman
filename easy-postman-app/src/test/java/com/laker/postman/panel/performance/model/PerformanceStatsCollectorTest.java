package com.laker.postman.panel.performance.model;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PerformanceStatsCollectorTest {

    @AfterMethod
    public void tearDown() {
        ApiMetadata.clear();
    }

    @Test
    public void shouldAggregateLargeRunsWithoutKeepingPerRequestResults() {
        ApiMetadata.register("search", "Search API");
        PerformanceStatsCollector collector = new PerformanceStatsCollector();

        for (int i = 0; i < 100_000; i++) {
            collector.record(new RequestResult(i, i + 100, true, "search", PerformanceProtocol.HTTP));
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
    public void shouldSampleTrendDataFromDeltasAndThenResetWindow() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        collector.record(new RequestResult(1_000, 1_100, true, "search", PerformanceProtocol.HTTP));
        collector.record(new RequestResult(1_100, 1_400, false, "search", PerformanceProtocol.HTTP));

        PerformanceTrendSnapshot first = collector.sampleTrendSnapshot(
                2_000,
                5,
                0,
                0,
                1_000,
                PerformanceRealtimeMetrics.Sample.empty()
        );
        PerformanceTrendSnapshot second = collector.sampleTrendSnapshot(
                3_000,
                5,
                0,
                0,
                1_000,
                PerformanceRealtimeMetrics.Sample.empty()
        );

        assertEquals(first.http().samples(), 2);
        assertEquals(first.http().failures(), 1);
        assertEquals(first.http().sampleRate(), 2.0);
        assertEquals(first.http().avgDurationMs(), 200.0);
        assertEquals(second.http().samples(), 0);
    }
}

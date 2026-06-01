package com.laker.postman.performance.core.model;

import org.testng.annotations.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
    public void shouldExposeLightweightProgressSnapshot() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();

        collector.record(new RequestResult(1_000L, 1_010L, true, "search", "Search API", PerformanceProtocol.HTTP));
        collector.record(new RequestResult(1_020L, 1_040L, false, "search", "Search API", PerformanceProtocol.HTTP));

        PerformanceStatsProgressSnapshot progress = collector.progressSnapshot();

        assertEquals(progress.totalRequests(), 2L);
        assertEquals(progress.successRequests(), 1L);
        assertEquals(progress.failedRequests(), 1L);
        assertEquals(progress.qps(), 50.0);
    }

    @Test
    public void shouldExposeSampleWindowAndByteThroughput() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();

        RequestResult first = new RequestResult(1_000L, 1_100L, true, "search", "Search API", PerformanceProtocol.HTTP);
        first.sentBytes = 1_000L;
        first.receivedBytes = 2_000L;
        collector.record(first);
        RequestResult second = new RequestResult(1_200L, 1_500L, true, "search", "Search API", PerformanceProtocol.HTTP);
        second.sentBytes = 500L;
        second.receivedBytes = 1_000L;
        collector.record(second);

        PerformanceStatsSnapshot.ApiSummary summary = collector.snapshot().summaries().get(0);

        assertEquals(summary.firstSampleStartTimeMs(), 1_000L);
        assertEquals(summary.lastSampleEndTimeMs(), 1_500L);
        assertEquals(summary.samplesPerSecond(), 4.0);
        assertEquals(summary.sentBytes(), 1_500L);
        assertEquals(summary.receivedBytes(), 3_000L);
        assertEquals(summary.sentBytesPerSecond(), 3_000.0);
        assertEquals(summary.receivedBytesPerSecond(), 6_000.0);
        assertEquals(summary.avgReceivedBytes(), 1_500L);
    }

    @Test
    public void statsCollectorShouldOnlyExposeReportAggregationApi() {
        assertFalse(hasMethodNamed(PerformanceStatsCollector.class, "setTrendEnabled"));
        assertFalse(hasMethodNamed(PerformanceStatsCollector.class, "sampleTrendSnapshot"));
    }

    @Test
    public void recordShouldNotUseCollectorWideSynchronizedMethodLock() throws Exception {
        assertFalse(Modifier.isSynchronized(PerformanceStatsCollector.class
                .getDeclaredMethod("record", RequestResult.class)
                .getModifiers()));
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

    @Test
    public void shouldAggregateConcurrentSamplesWithoutLostUpdates() throws Exception {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        int threads = 8;
        int samplesPerThread = 1_000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Thread> workers = new ArrayList<>();

        for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < samplesPerThread; i++) {
                        collector.record(new RequestResult(i, i + 10, true, "search", "Search API", PerformanceProtocol.HTTP));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            workers.add(worker);
            worker.start();
        }

        start.countDown();
        done.await();

        PerformanceStatsSnapshot snapshot = collector.snapshot();

        assertEquals(snapshot.totalRequests(), (long) threads * samplesPerThread);
        assertEquals(snapshot.successRequests(), (long) threads * samplesPerThread);
        assertEquals(snapshot.summaries().get(0).durationStats().avg(), 10L);
    }

    private static boolean hasMethodNamed(Class<?> type, String methodName) {
        return java.util.Arrays.stream(type.getMethods())
                .anyMatch(method -> methodName.equals(method.getName()));
    }
}

package com.laker.postman.panel.performance.control;

import com.laker.postman.panel.performance.result.PerformanceTrendView;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;
import org.jfree.data.time.RegularTimePeriod;
import org.testng.annotations.Test;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceStatisticsCoordinatorAsyncTimestampTest {

    @Test
    public void asyncTrendSampleShouldUseActualDrainTimeAsWindowTimestamp() throws Exception {
        CapturingTrendView trendView = new CapturingTrendView();
        AtomicLong realtimeSamplerNow = new AtomicLong();
        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                new PerformanceStatsCollector(),
                new PerformanceTrendWindowCollector(),
                null,
                trendView,
                new JTabbedPane(),
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                () -> true,
                now -> {
                    realtimeSamplerNow.set(now);
                    return PerformanceRealtimeMetrics.Sample.empty();
                },
                now -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );

        try {
            CountDownLatch releaseExecutor = new CountDownLatch(1);
            Future<?> blockingTask = metricsExecutor(coordinator).submit(() -> await(releaseExecutor));

            coordinator.sampleTrendData();
            Thread.sleep(120);
            long releaseTimeMs = System.currentTimeMillis();
            releaseExecutor.countDown();
            blockingTask.get(1, TimeUnit.SECONDS);
            waitForQueuedMetrics(coordinator);
            assertTrue(trendView.awaitUpdate());
            SwingUtilities.invokeAndWait(() -> {
            });

            assertTrue(realtimeSamplerNow.get() >= releaseTimeMs,
                    "趋势窗口应使用实际 drain 时间采样，realtimeSamplerNow=" + realtimeSamplerNow.get()
                            + ", releaseTimeMs=" + releaseTimeMs);
            assertTrue(trendView.period.getFirstMillisecond() >= releaseTimeMs,
                    "趋势图 X 轴应使用实际 drain 时间，period=" + trendView.period.getFirstMillisecond()
                            + ", releaseTimeMs=" + releaseTimeMs);
        } finally {
            coordinator.dispose();
        }
    }

    @Test
    public void syncTrendSampleShouldUseExplicitTimestampWhenProvided() throws Exception {
        CapturingTrendView trendView = new CapturingTrendView();
        AtomicLong realtimeSamplerNow = new AtomicLong();
        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                new PerformanceStatsCollector(),
                new PerformanceTrendWindowCollector(),
                null,
                trendView,
                new JTabbedPane(),
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                () -> true,
                now -> {
                    realtimeSamplerNow.set(now);
                    return PerformanceRealtimeMetrics.Sample.empty();
                },
                now -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );

        try {
            long sampleTimeMs = 1_776_000_123_456L;

            SwingUtilities.invokeAndWait(() -> coordinator.sampleTrendDataSync(sampleTimeMs));

            assertTrue(trendView.awaitUpdate());
            assertEquals(trendView.period.getFirstMillisecond(), sampleTimeMs);
            assertEquals(realtimeSamplerNow.get(), sampleTimeMs);
        } finally {
            coordinator.dispose();
        }
    }

    private static ExecutorService metricsExecutor(PerformanceStatisticsCoordinator coordinator) throws Exception {
        Field field = PerformanceStatisticsCoordinator.class.getDeclaredField("metricsExecutor");
        field.setAccessible(true);
        return (ExecutorService) field.get(coordinator);
    }

    private static void waitForQueuedMetrics(PerformanceStatisticsCoordinator coordinator) throws Exception {
        Future<?> future = metricsExecutor(coordinator).submit(() -> {
        });
        future.get(1, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class CapturingTrendView implements PerformanceTrendView {
        private final CountDownLatch updated = new CountDownLatch(1);
        private volatile RegularTimePeriod period;

        @Override
        public void clearTrendDataset() {
        }

        @Override
        public void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot) {
            this.period = period;
            updated.countDown();
        }

        private boolean awaitUpdate() throws InterruptedException {
            return updated.await(1, TimeUnit.SECONDS);
        }
    }
}

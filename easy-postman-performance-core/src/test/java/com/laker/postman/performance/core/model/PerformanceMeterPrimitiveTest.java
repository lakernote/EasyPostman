package com.laker.postman.performance.core.model;

import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceMeterPrimitiveTest {

    @Test
    public void counterShouldOnlyIncreaseByNonNegativeDelta() {
        PerformanceCounter counter = new PerformanceCounter();

        counter.increment(3);
        counter.increment(-10);

        assertEquals(counter.count(), 3L);
    }

    @Test
    public void timerShouldExposeCountMeanAndPercentileSnapshot() {
        PerformanceTimer timer = new PerformanceTimer();

        timer.record(100);
        timer.record(300);

        assertEquals(timer.count(), 2L);
        assertEquals(timer.meanMs(), 200.0);
        assertEquals(timer.snapshot().p95(), 300L);
    }

    @Test
    public void distributionSummaryShouldTrackTotalAndMean() {
        PerformanceDistributionSummary summary = new PerformanceDistributionSummary();

        summary.record(512);
        summary.record(1_536);

        assertEquals(summary.count(), 2L);
        assertEquals(summary.totalAmount(), 2_048L);
        assertEquals(summary.mean(), 1_024.0);
    }

    @Test
    public void sampleTimeWindowShouldTrackGlobalSampleSpan() {
        PerformanceSampleTimeWindow window = new PerformanceSampleTimeWindow();

        window.record(1_000L, 1_100L);
        window.record(1_200L, 1_500L);

        assertEquals(window.firstStartMs(), 1_000L);
        assertEquals(window.lastEndMs(), 1_500L);
        assertEquals(window.spanSeconds(), 0.5);
    }

    @Test
    public void meterPrimitivesShouldRemainPackagePrivate() {
        assertFalse(Modifier.isPublic(PerformanceCounter.class.getModifiers()));
        assertFalse(Modifier.isPublic(PerformanceTimer.class.getModifiers()));
        assertFalse(Modifier.isPublic(PerformanceDistributionSummary.class.getModifiers()));
        assertFalse(Modifier.isPublic(PerformanceSampleTimeWindow.class.getModifiers()));
        assertFalse(Modifier.isPublic(PerformanceSampleMeterSet.class.getModifiers()));
    }

    @Test
    public void sampleMeterSetShouldSynchronizeMutationAndSnapshot() throws Exception {
        assertSynchronizedMethod("record", RequestResult.class);
        assertSynchronizedMethod("clear");
        assertSynchronizedMethod("snapshot");
        assertSynchronizedMethod("toSummary", String.class);
        assertSynchronizedMethod("toProgressSnapshot");
    }

    private static void assertSynchronizedMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = PerformanceSampleMeterSet.class.getDeclaredMethod(name, parameterTypes);
        assertTrue(Modifier.isSynchronized(method.getModifiers()), name + " must stay synchronized");
    }
}

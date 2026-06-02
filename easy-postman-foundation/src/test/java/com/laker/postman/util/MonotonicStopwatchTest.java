package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MonotonicStopwatchTest {

    @Test
    public void shouldMeasureElapsedTimeFromMonotonicNanos() {
        MonotonicStopwatch stopwatch = MonotonicStopwatch.startedAt(1_000L, 10_000_000L);

        assertEquals(stopwatch.elapsedMs(130_000_000L), 120L);
        assertEquals(stopwatch.projectedEndTimeMs(130_000_000L), 1_120L);
    }

    @Test
    public void shouldClampBackwardNanoReadingsToZero() {
        MonotonicStopwatch stopwatch = MonotonicStopwatch.startedAt(1_000L, 130_000_000L);

        assertEquals(stopwatch.elapsedMs(10_000_000L), 0L);
        assertEquals(stopwatch.projectedEndTimeMs(10_000_000L), 1_000L);
    }
}

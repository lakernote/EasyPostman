package com.laker.postman.panel.performance.result;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class PerformanceTrendSeriesValueTest {

    @Test
    public void shouldKeepZeroForActiveCounts() {
        assertEquals(PerformanceTrendSeriesValue.activeCount(0).intValue(), 0);
        assertEquals(PerformanceTrendSeriesValue.activeCount(-3).intValue(), 0);
    }

    @Test
    public void shouldUseNullForMissingSampleMetrics() {
        assertEquals(PerformanceTrendSeriesValue.sampleMetric(12.5).doubleValue(), 12.5);
        assertNull(PerformanceTrendSeriesValue.sampleMetric(Double.NaN));
        assertNull(PerformanceTrendSeriesValue.sampleMetric(Double.POSITIVE_INFINITY));
    }
}

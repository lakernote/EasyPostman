package com.laker.postman.panel.performance.result;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceTrendSparsePointPolicyTest {

    @Test
    public void shouldShowOnlyIsolatedFiniteSamplesInSparseSeries() {
        TimeSeries series = new TimeSeries("响应时间");
        long base = 1_700_000_000_000L;
        series.add(new Millisecond(new Date(base)), 3_200.0);
        series.add(new Millisecond(new Date(base + 1_000)), null);
        series.add(new Millisecond(new Date(base + 2_000)), 1_400.0);
        series.add(new Millisecond(new Date(base + 3_000)), 1_300.0);
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        assertTrue(PerformanceTrendSparsePointPolicy.shouldShowShape(dataset, 0, 0));
        assertFalse(PerformanceTrendSparsePointPolicy.shouldShowShape(dataset, 0, 1));
        assertFalse(PerformanceTrendSparsePointPolicy.shouldShowShape(dataset, 0, 2));
        assertFalse(PerformanceTrendSparsePointPolicy.shouldShowShape(dataset, 0, 3));
    }

    @Test
    public void shouldShowSingleFiniteSample() {
        TimeSeries series = new TimeSeries("响应时间");
        series.add(new Millisecond(new Date(1_700_000_000_000L)), 69.0);
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        assertTrue(PerformanceTrendSparsePointPolicy.shouldShowShape(dataset, 0, 0));
    }
}

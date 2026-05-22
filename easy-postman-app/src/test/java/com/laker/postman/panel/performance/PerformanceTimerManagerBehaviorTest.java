package com.laker.postman.panel.performance;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class PerformanceTimerManagerBehaviorTest {

    @Test
    public void shouldNotScheduleTrendSamplingWhenTrendDisabledBeforeStart() throws Exception {
        PerformanceTimerManager manager = new PerformanceTimerManager(() -> true);
        manager.setTrendSamplingCallback(() -> {
            throw new AssertionError("Trend sampling should not run when disabled");
        });
        manager.setReportRefreshCallback(() -> {
        });
        manager.setTrendSamplingEnabled(false);

        try {
            manager.startAll();
            assertNull(readField(manager, "trendSamplingTask"));
        } finally {
            manager.dispose();
        }
    }

    @Test
    public void shouldNotScheduleReportRefreshByDefault() throws Exception {
        PerformanceTimerManager manager = new PerformanceTimerManager(() -> true);
        manager.setTrendSamplingCallback(() -> {
        });
        manager.setReportRefreshCallback(() -> {
        });

        try {
            manager.startAll();
            assertNull(readField(manager, "reportRefreshTask"));
        } finally {
            manager.dispose();
        }
    }

    @Test
    public void shouldScheduleReportRefreshWhenRealtimeReportEnabledBeforeStart() throws Exception {
        PerformanceTimerManager manager = new PerformanceTimerManager(() -> true);
        manager.setTrendSamplingCallback(() -> {
        });
        manager.setReportRefreshCallback(() -> {
        });
        manager.setReportRefreshEnabled(true);

        try {
            manager.startAll();
            assertNotNull(readField(manager, "reportRefreshTask"));
        } finally {
            manager.dispose();
        }
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}

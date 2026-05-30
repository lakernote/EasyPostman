package com.laker.postman.panel.performance;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceRemoteRunControlSupportTest {

    @Test
    public void shouldRequestWorkerReportWhenTrendIsEnabled() {
        PerformanceRemoteRunControlSupport support = support(true, false);

        assertTrue(support.shouldIncludeStatusReport());
    }

    @Test
    public void shouldRequestWorkerReportWhenRealtimeReportIsEnabled() {
        PerformanceRemoteRunControlSupport support = support(false, true);

        assertTrue(support.shouldIncludeStatusReport());
    }

    @Test
    public void shouldSkipWorkerReportWhenTrendAndRealtimeReportAreDisabled() {
        PerformanceRemoteRunControlSupport support = support(false, false);

        assertFalse(support.shouldIncludeStatusReport());
    }

    private static PerformanceRemoteRunControlSupport support(boolean trendEnabled,
                                                             boolean reportRealtimeEnabled) {
        return new PerformanceRemoteRunControlSupport(
                () -> false,
                ignored -> {
                },
                null,
                null,
                null,
                null,
                () -> {
                },
                () -> {
                },
                () -> trendEnabled,
                () -> reportRealtimeEnabled,
                () -> 1_000L
        );
    }
}

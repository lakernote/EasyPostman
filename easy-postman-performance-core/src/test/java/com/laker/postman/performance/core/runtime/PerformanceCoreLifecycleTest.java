package com.laker.postman.performance.core.runtime;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PerformanceCoreLifecycleTest {

    @Test
    public void shouldExposeHeadlessRunLifecycleDtosInsideCoreModule() {
        PerformanceRunProgress progress = new PerformanceRunProgress(3, 10);
        PerformanceRunSummary summary = PerformanceRunSummary.builder()
                .startTimeMs(1_000)
                .endTimeMs(1_250)
                .elapsedTimeMs(250)
                .stopped(false)
                .build();

        assertEquals(progress.getActiveThreads(), 3);
        assertEquals(progress.getTotalThreads(), 10);
        assertEquals(progress.getSequence(), 0L);
        assertEquals(summary.getElapsedTimeMs(), 250);
        assertFalse(summary.isStopped());
    }
}

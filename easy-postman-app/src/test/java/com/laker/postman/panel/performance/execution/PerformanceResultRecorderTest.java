package com.laker.postman.panel.performance.execution;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceResultRecorderTest {

    @Test
    public void shouldOnlyRecordSlowOrFailedResultsInEfficientMode() {
        assertFalse(PerformanceResultRecorder.shouldRecordResult(true, true, 1200, 3000));
        assertTrue(PerformanceResultRecorder.shouldRecordResult(true, true, 3000, 3000));
        assertTrue(PerformanceResultRecorder.shouldRecordResult(true, false, 100, 3000));
        assertTrue(PerformanceResultRecorder.shouldRecordResult(false, true, 100, 3000));
        assertFalse(PerformanceResultRecorder.shouldRecordResult(true, true, 5000, 0));
    }
}

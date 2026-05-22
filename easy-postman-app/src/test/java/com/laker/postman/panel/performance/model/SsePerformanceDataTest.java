package com.laker.postman.panel.performance.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SsePerformanceDataTest {

    @Test
    public void shouldUseEventNameFilterOnlyForMessageDrivenModes() {
        assertFalse(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.FIRST_MESSAGE));
        assertTrue(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.MATCHED_MESSAGE));
        assertTrue(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.MESSAGE_COUNT));
        assertFalse(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.FIXED_DURATION));
    }
}

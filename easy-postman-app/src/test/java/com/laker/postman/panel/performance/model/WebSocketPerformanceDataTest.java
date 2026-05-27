package com.laker.postman.panel.performance.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebSocketPerformanceDataTest {

    @Test
    public void shouldNormalizeLegacyCompletionModesForStorageReads() {
        assertEquals(WebSocketPerformanceData.completionModeFromStorageValue("FIRST_MESSAGE"),
                WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE);
        assertEquals(WebSocketPerformanceData.completionModeFromStorageValue("MATCHED_MESSAGE"),
                WebSocketPerformanceData.CompletionMode.UNTIL_MATCH);
        assertEquals(WebSocketPerformanceData.completionModeFromStorageValue("unknown"),
                WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE);
    }

    @Test
    public void shouldOnlyUseReadFilterForFilterBasedModes() {
        assertFalse(WebSocketPerformanceData.usesMessageFilter(WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE));
        assertTrue(WebSocketPerformanceData.usesMessageFilter(WebSocketPerformanceData.CompletionMode.UNTIL_MATCH));
        assertTrue(WebSocketPerformanceData.usesMessageFilter(WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT));
    }
}

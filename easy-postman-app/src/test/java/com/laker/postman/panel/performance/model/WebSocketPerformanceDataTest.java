package com.laker.postman.panel.performance.model;

import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebSocketPerformanceDataTest {

    @Test
    public void shouldUseSingleMessageAsDefaultCompletionMode() {
        WebSocketPerformanceData data = new WebSocketPerformanceData();

        assertEquals(data.completionMode, WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE);
    }

    @Test
    public void shouldOnlyUseReadFilterForFilterBasedModes() {
        assertFalse(WebSocketPerformanceData.usesMessageFilter(WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE));
        assertTrue(WebSocketPerformanceData.usesMessageFilter(WebSocketPerformanceData.CompletionMode.UNTIL_MATCH));
        assertTrue(WebSocketPerformanceData.usesMessageFilter(WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT));
    }
}

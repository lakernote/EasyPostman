package com.laker.postman.panel.performance.model;

import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SsePerformanceDataTest {

    @Test
    public void shouldUseEventNameFilterOnlyForMessageDrivenModes() {
        assertFalse(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.SINGLE_MESSAGE));
        assertTrue(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.UNTIL_MATCH));
        assertTrue(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.MESSAGE_COUNT));
        assertFalse(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.FIXED_DURATION));
        assertFalse(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.STREAM_CLOSED));
    }

    @Test
    public void shouldDescribeSseReceiveCompletionAsClosingStream() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_SSE_READ_MODE), "读取方式");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_SSE_NODE_READ), "SSE Read");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_MENU_ADD_SSE_READ), "添加 SSE Read");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_SSE_HINT_FIRST_MESSAGE),
                "收到首事件即结束；超时失败。");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_SSE_READ_MODE), "Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_SSE_NODE_READ), "SSE Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_MENU_ADD_SSE_READ), "Add SSE Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_SSE_HINT_FIRST_MESSAGE),
                "Finish on the first event; timeout fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_SSE_HINT_STREAM_CLOSED),
                "Finish when the server closes; timeout fails.");
    }
}

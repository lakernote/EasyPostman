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
        assertFalse(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.FIRST_MESSAGE));
        assertTrue(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.MATCHED_MESSAGE));
        assertTrue(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.MESSAGE_COUNT));
        assertFalse(SsePerformanceData.usesEventNameFilter(SsePerformanceData.CompletionMode.FIXED_DURATION));
    }

    @Test
    public void shouldDescribeSseReceiveCompletionAsClosingStream() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_SSE_AWAIT_MODE), "接收结束条件");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_SSE_HINT_FIRST_MESSAGE),
                "连接成功后等待第一条真实 SSE 事件，收到即结束本次采样并关闭 SSE 连接；不受事件名或内容过滤影响；超时未收到则失败。");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_SSE_AWAIT_MODE), "Receive Completion");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_SSE_HINT_FIRST_MESSAGE),
                "After the connection opens, wait for the first real SSE event. Event and message filters are ignored in this mode. The sample finishes and closes the SSE stream when it arrives, or fails on timeout.");
    }
}

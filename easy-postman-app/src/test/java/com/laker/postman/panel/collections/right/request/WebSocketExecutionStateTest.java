package com.laker.postman.panel.collections.right.request;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebSocketExecutionStateTest {

    @Test(description = "正常 close 后应允许写入 history")
    public void testShouldSaveHistoryAfterClose() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertFalse(state.shouldSaveHistory(false));

        assertTrue(state.markClosed());
        assertTrue(state.shouldSaveHistory(false));
    }

    @Test(description = "failure 后应允许写入 history")
    public void testShouldSaveHistoryAfterFailure() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertTrue(state.markFailed());
        assertTrue(state.shouldSaveHistory(false));
    }

    @Test(description = "连接终态后 history 判定不应依赖 current connection id")
    public void testShouldSaveHistoryAfterReconnect() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertTrue(state.shouldHandleActiveCallback("conn-1", false));

        assertTrue(state.markClosed());

        assertFalse(state.shouldHandleActiveCallback("conn-2", false));
        assertTrue(state.shouldSaveHistory(false));
    }

    @Test(description = "disposed 面板不应写入 history")
    public void testShouldNotSaveHistoryWhenDisposed() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertTrue(state.markClosed());
        assertFalse(state.shouldSaveHistory(true));
    }
}

package com.laker.postman.panel.performance.execution;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.testng.Assert.assertEquals;

public class WebSocketScenarioExecutorTest {

    @Test
    public void shouldBuildResponseBodyFromAllReceivedMessages() {
        String body = WebSocketScenarioExecutor.buildResponseBody(Arrays.asList("first", "second"));

        assertEquals(body, "first\n\nsecond\n\n");
    }

    @Test
    public void shouldReturnEmptyBodyWhenNoMessagesReceived() {
        String body = WebSocketScenarioExecutor.buildResponseBody(Collections.emptyList());

        assertEquals(body, "");
    }
}

package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureFilterStateTest {

    @Test
    public void shouldApplyLatestFilterWithoutRecreatingHandlers() {
        CaptureFilterState state = new CaptureFilterState();

        assertTrue(state.current().matches(
                "api.example.com",
                "/orders",
                "https://api.example.com/orders",
                Map.of()));

        state.update("host:chatgpt.com");

        assertTrue(state.current().matches(
                "chatgpt.com",
                "/ces/v1/m",
                "https://chatgpt.com/ces/v1/m",
                Map.of()));
        assertFalse(state.current().matches(
                "api.example.com",
                "/orders",
                "https://api.example.com/orders",
                Map.of()));
    }
}

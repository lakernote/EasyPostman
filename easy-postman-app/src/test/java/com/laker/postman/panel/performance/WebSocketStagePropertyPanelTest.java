package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;

public class WebSocketStagePropertyPanelTest {

    @Test
    public void shouldResolveAwaitModeHintForEveryCompletionMode() {
        for (WebSocketPerformanceData.CompletionMode mode : WebSocketPerformanceData.CompletionMode.values()) {
            String hint = WebSocketStagePropertyPanel.resolveAwaitModeHint(mode);

            assertFalse(hint == null || hint.isBlank(), mode.name());
        }
    }
}

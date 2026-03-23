package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class CapturePanelTest {

    @Test
    public void shouldReturnDraftSummaryForIncompleteFilterExpression() {
        String summary = CapturePanel.summarizeDraftCaptureFilter("(a.com or");

        assertTrue(summary.contains("a.com or"));
    }
}

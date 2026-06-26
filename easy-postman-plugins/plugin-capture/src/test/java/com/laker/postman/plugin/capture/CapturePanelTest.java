package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class CapturePanelTest {

    @Test
    public void shouldReturnDraftSummaryForIncompleteFilterExpression() {
        String summary = CapturePanel.summarizeDraftCaptureFilter("(a.com or");

        assertTrue(summary.contains("a.com or"));
    }

    @Test
    public void shouldRetainSelectedFlowWhenItRemainsVisibleAfterTableRefresh() {
        CaptureFlow selected = new CaptureFlow(
                "GET",
                "https://example.com/selected",
                "example.com",
                "/selected",
                Map.of(),
                new byte[0]
        );
        CaptureFlow newer = new CaptureFlow(
                "GET",
                "https://example.com/newer",
                "example.com",
                "/newer",
                Map.of(),
                new byte[0]
        );

        CaptureFlow retained = CapturePanel.findVisibleSelectedFlow(selected, List.of(newer, selected));

        assertSame(retained, selected);
    }

    @Test
    public void shouldDropSelectedFlowWhenItIsNoLongerVisibleAfterTableRefresh() {
        CaptureFlow selected = new CaptureFlow(
                "GET",
                "https://example.com/selected",
                "example.com",
                "/selected",
                Map.of(),
                new byte[0]
        );
        CaptureFlow newer = new CaptureFlow(
                "GET",
                "https://example.com/newer",
                "example.com",
                "/newer",
                Map.of(),
                new byte[0]
        );

        CaptureFlow retained = CapturePanel.findVisibleSelectedFlow(selected, List.of(newer));

        assertNull(retained);
    }

    @Test
    public void shouldFormatRetainedRowCountWithCapacity() {
        String countText = CapturePanel.formatRetainedRowCount(37, 37, 300);

        assertTrue(countText.contains("37"));
        assertTrue(countText.contains("300"));
    }
}

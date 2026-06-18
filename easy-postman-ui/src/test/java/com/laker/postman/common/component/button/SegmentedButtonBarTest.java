package com.laker.postman.common.component.button;

import org.testng.annotations.Test;

import java.awt.FlowLayout;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SegmentedButtonBarTest {
    @Test
    public void shouldManageExclusiveSelectionByValue() {
        SegmentedButtonBar<String> bar = new SegmentedButtonBar<>(FlowLayout.LEFT, SegmentedButtonBar.Size.COMPACT);
        SegmentedToggleButton countButton = bar.addOption("count", "次数", true);
        SegmentedToggleButton timeButton = bar.addOption("time", "时间", false);
        AtomicReference<String> selected = new AtomicReference<>();
        bar.setSelectionListener(selected::set);

        timeButton.doClick();

        assertEquals(bar.getSelectedValue(), "time");
        assertEquals(selected.get(), "time");
        assertFalse(countButton.isSelected());
        assertTrue(timeButton.isSelected());
        assertEquals(bar.getButton("count"), countButton);
    }

    @Test
    public void compactSizeShouldFitDenseForms() {
        SegmentedButtonBar<String> bar = new SegmentedButtonBar<>(FlowLayout.LEFT, SegmentedButtonBar.Size.COMPACT);
        bar.addOption("count", "次数", true);
        bar.addOption("time", "时间", false);

        assertTrue(bar.getPreferredSize().height <= 29,
                "compact segmented button bar should align with dense 28px controls, preferred height: "
                        + bar.getPreferredSize().height);
        assertNotNull(bar.getBorder());
    }

    @Test
    public void defaultSizeShouldStayToolbarCompact() {
        SegmentedButtonBar<String> bar = new SegmentedButtonBar<>(FlowLayout.LEFT);
        bar.addOption("trend", "趋势", true);
        bar.addOption("report", "报表", false);
        bar.addOption("table", "结果表", false);

        assertTrue(bar.getPreferredSize().height <= 31,
                "default segmented button bar should stay compact in toolbars, preferred height: "
                        + bar.getPreferredSize().height);
    }

    @Test
    public void defaultSegmentsShouldUseNaturalContentWidth() {
        SegmentedButtonBar<String> bar = new SegmentedButtonBar<>(FlowLayout.LEFT);
        SegmentedToggleButton httpButton = bar.addOption("http", "HTTP", true);
        SegmentedToggleButton webSocketButton = bar.addOption("ws", "WebSocket", false);
        SegmentedToggleButton sseButton = bar.addOption("sse", "SSE", false);

        assertTrue(httpButton.getPreferredSize().width < webSocketButton.getPreferredSize().width);
        assertTrue(sseButton.getPreferredSize().width < webSocketButton.getPreferredSize().width);
    }

    @Test
    public void compactSegmentsShouldUseConsistentWidthByDefault() {
        SegmentedButtonBar<String> bar = new SegmentedButtonBar<>(FlowLayout.LEFT, SegmentedButtonBar.Size.COMPACT);
        SegmentedToggleButton countButton = bar.addOption("count", "次数", true);
        SegmentedToggleButton timeButton = bar.addOption("time", "时间", false);

        assertEquals(countButton.getPreferredSize().width, timeButton.getPreferredSize().width);
    }

    @Test
    public void equalWidthCanStillBeEnabledForDefaultBars() {
        SegmentedButtonBar<String> bar = new SegmentedButtonBar<>(FlowLayout.LEFT);
        SegmentedToggleButton httpButton = bar.addOption("http", "HTTP", true);
        SegmentedToggleButton webSocketButton = bar.addOption("ws", "WebSocket", false);
        SegmentedToggleButton sseButton = bar.addOption("sse", "SSE", false);

        bar.setEqualSegmentWidth(true);

        assertEquals(httpButton.getPreferredSize().width, webSocketButton.getPreferredSize().width);
        assertEquals(sseButton.getPreferredSize().width, webSocketButton.getPreferredSize().width);
    }
}

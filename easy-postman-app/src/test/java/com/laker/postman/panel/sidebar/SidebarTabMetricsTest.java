package com.laker.postman.panel.sidebar;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SidebarTabMetricsTest {

    @Test
    public void shouldClampExpandedTabWidth() {
        assertEquals(SidebarTabMetrics.expandedWidth(20), 60);
        assertEquals(SidebarTabMetrics.expandedWidth(80), 104);
        assertEquals(SidebarTabMetrics.expandedWidth(200), 140);
    }

    @Test
    public void shouldCalculateCollapsedTabWidthFromIconWidth() {
        assertEquals(SidebarTabMetrics.collapsedWidth(10), 44);
        assertEquals(SidebarTabMetrics.collapsedWidth(24), 46);
    }

    @Test
    public void shouldCalculateTabHeights() {
        assertEquals(SidebarTabMetrics.expandedHeight(20, 14), 60);
        assertEquals(SidebarTabMetrics.expandedHeight(40, 24), 86);
        assertEquals(SidebarTabMetrics.collapsedHeight(20), 40);
        assertEquals(SidebarTabMetrics.collapsedHeight(40), 56);
    }

    @Test
    public void collapsedSelectedBackgroundShouldStayCompactRoundedRectangle() {
        assertEquals(SidebarTabMetrics.COLLAPSED_SELECTED_BACKGROUND_WIDTH, 34);
        assertEquals(SidebarTabMetrics.COLLAPSED_SELECTED_BACKGROUND_HEIGHT, 32);
        assertEquals(SidebarTabMetrics.SELECTED_BACKGROUND_ARC, 10);
    }
}

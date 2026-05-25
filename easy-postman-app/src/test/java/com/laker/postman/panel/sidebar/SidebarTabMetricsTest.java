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
        assertEquals(SidebarTabMetrics.collapsedWidth(10), 40);
        assertEquals(SidebarTabMetrics.collapsedWidth(24), 44);
    }

    @Test
    public void shouldCalculateTabHeights() {
        assertEquals(SidebarTabMetrics.expandedHeight(20, 14), 70);
        assertEquals(SidebarTabMetrics.expandedHeight(40, 24), 97);
        assertEquals(SidebarTabMetrics.collapsedHeight(20), 52);
        assertEquals(SidebarTabMetrics.collapsedHeight(40), 68);
    }
}

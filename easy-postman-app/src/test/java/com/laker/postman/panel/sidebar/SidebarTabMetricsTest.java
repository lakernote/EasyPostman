package com.laker.postman.panel.sidebar;

import com.laker.postman.util.IconUtil;
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
    public void collapsedSelectedBackgroundShouldStayCompactWithoutInflatingSelectedTab() {
        assertEquals(SidebarTabMetrics.COLLAPSED_SELECTED_BACKGROUND_WIDTH, 32);
        assertEquals(SidebarTabMetrics.COLLAPSED_SELECTED_BACKGROUND_HEIGHT, 32);
        assertEquals(SidebarTabMetrics.SELECTED_BACKGROUND_ARC, 10);
    }

    @Test
    public void collapsedSelectedBackgroundShouldBeGeometricallyCenteredInCollapsedRail() {
        assertEquals(SidebarTabMetrics.TAB_AREA_INSET_TOP, 4);
        assertEquals(SidebarTabMetrics.COLLAPSED_VISUAL_CENTER_OFFSET_X, 0);
        assertEquals(SidebarTabMetrics.collapsedSelectedBackgroundX(4, 44, 32), 10);
        assertEquals(SidebarTabMetrics.collapsedSelectedBackgroundX(4, 32, 32), 4);
    }

    @Test
    public void collapsedIconPaddingShouldStaySymmetricAroundSelectedBackground() {
        assertEquals(SidebarTabMetrics.collapsedIconPaddingLeft(), 11);
        assertEquals(SidebarTabMetrics.collapsedIconPaddingRight(), 11);
    }

    @Test
    public void sidebarTabIconShouldStayLightweightInCollapsedToolWindowStripe() {
        assertEquals(IconUtil.SIZE_TAB, 20);
    }
}

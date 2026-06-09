package com.laker.postman.common.component;

import com.laker.postman.common.component.button.PlusButton;
import org.testng.annotations.Test;

import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ToolWindowSidebarToolbarTest {

    @Test
    public void shouldUseConsistentSidebarToolbarInsetsAndControlHeights() {
        PlusButton plusButton = new PlusButton();
        SearchTextField searchField = new SearchTextField();

        ToolWindowSidebarToolbar toolbar = new ToolWindowSidebarToolbar(plusButton, searchField);

        assertTrue(toolbar.getBorder() instanceof EmptyBorder);
        assertEquals(toolbar.getInsets().top, ToolWindowSidebarToolbar.VERTICAL_PADDING);
        assertEquals(toolbar.getInsets().left, ToolWindowSidebarToolbar.HORIZONTAL_PADDING);
        assertEquals(toolbar.getInsets().bottom, ToolWindowSidebarToolbar.VERTICAL_PADDING);
        assertEquals(toolbar.getInsets().right, ToolWindowSidebarToolbar.HORIZONTAL_PADDING);

        assertSame(toolbar.getComponent(0), plusButton);
        assertTrue(toolbar.getComponent(1) instanceof Box.Filler);
        assertSame(toolbar.getComponent(2), searchField);
        assertEquals(plusButton.getPreferredSize(), new Dimension(
                ToolWindowSidebarToolbar.ACTION_SIZE,
                ToolWindowSidebarToolbar.ACTION_SIZE
        ));
        assertEquals(searchField.getPreferredSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertEquals(searchField.getMaximumSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertEquals(plusButton.getAlignmentY(), Component.CENTER_ALIGNMENT);
        assertEquals(searchField.getAlignmentY(), Component.CENTER_ALIGNMENT);
    }

    @Test
    public void shouldSupportSearchOnlySidebarToolbars() {
        SearchTextField searchField = new SearchTextField();

        ToolWindowSidebarToolbar toolbar = new ToolWindowSidebarToolbar(null, searchField);

        assertEquals(toolbar.getComponentCount(), 1);
        assertSame(toolbar.getComponent(0), searchField);
        assertEquals(searchField.getPreferredSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertEquals(searchField.getMaximumSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
    }
}

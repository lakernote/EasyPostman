package com.laker.postman.common.component;

import com.laker.postman.common.component.button.PlusButton;
import org.testng.annotations.Test;

import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
                ToolWindowStripeMetrics.ACTION_SIZE,
                ToolWindowStripeMetrics.ACTION_SIZE
        ));
        assertEquals(ToolWindowSidebarToolbar.ACTION_SIZE, ToolWindowStripeMetrics.ACTION_SIZE);
        assertEquals(ToolWindowStripeMetrics.STRIPE_THICKNESS, ToolWindowStripeMetrics.ACTION_SIZE);
        assertEquals(ToolWindowSidebarToolbar.SEARCH_HEIGHT, 30);
        assertEquals(searchField.getPreferredSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertEquals(searchField.getMaximumSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertTrue(toolbar.getPreferredSize().width <= ToolWindowChrome.DEFAULT_SIDE_WIDTH);
        assertEquals(plusButton.getAlignmentY(), Component.CENTER_ALIGNMENT);
        assertEquals(searchField.getAlignmentY(), Component.CENTER_ALIGNMENT);
        assertFalse(searchField.isFocusable());
    }

    @Test
    public void shouldSupportSearchOnlySidebarToolbars() {
        SearchTextField searchField = new SearchTextField();

        ToolWindowSidebarToolbar toolbar = new ToolWindowSidebarToolbar(null, searchField);

        assertEquals(toolbar.getComponentCount(), 1);
        assertSame(toolbar.getComponent(0), searchField);
        assertEquals(searchField.getPreferredSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertEquals(searchField.getMaximumSize().height, ToolWindowSidebarToolbar.SEARCH_HEIGHT);
        assertFalse(searchField.isFocusable());
    }

    @Test
    public void shouldActivateSearchFocusOnlyAfterUserMouseIntent() {
        SearchTextField searchField = new SearchTextField();
        new ToolWindowSidebarToolbar(null, searchField);

        assertFalse(searchField.isFocusable());

        MouseEvent mousePressed = new MouseEvent(
                searchField,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                4,
                4,
                1,
                false
        );
        for (var listener : searchField.getMouseListeners()) {
            listener.mousePressed(mousePressed);
        }

        assertTrue(searchField.isFocusable());
    }
}

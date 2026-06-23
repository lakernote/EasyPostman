package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.JToolBar;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SearchTextFieldTest {

    @Test
    public void shouldOnlyJoinFocusTraversalAfterUserMouseIntent() {
        SearchTextField searchField = new SearchTextField();

        searchField.installUserActivatedFocus();

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

    @Test
    public void shouldLeaveFocusTraversalAgainWhenEmptyAfterFocusLost() {
        SearchTextField searchField = new SearchTextField();
        searchField.installUserActivatedFocus();

        for (var listener : searchField.getMouseListeners()) {
            listener.mousePressed(new MouseEvent(
                    searchField,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    4,
                    4,
                    1,
                    false
            ));
        }
        assertTrue(searchField.isFocusable());

        FocusEvent focusLost = new FocusEvent(searchField, FocusEvent.FOCUS_LOST);
        for (var listener : searchField.getFocusListeners()) {
            listener.focusLost(focusLost);
        }

        assertFalse(searchField.isFocusable());
    }

    @Test
    public void shouldUseCompactInTextFieldOptionButtons() {
        SearchTextField searchField = new SearchTextField();

        Object trailingComponent = searchField.getClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT);

        assertTrue(trailingComponent instanceof JToolBar);
        JToolBar toolbar = (JToolBar) trailingComponent;
        assertFalse(toolbar.isOpaque());
        assertEquals(toolbar.getClientProperty(FlatClientProperties.STYLE_CLASS), "inTextField");
        assertEquals(toolbar.getComponentCount(), 2);
        for (Component component : toolbar.getComponents()) {
            assertTrue(component instanceof AbstractButton);
            AbstractButton button = (AbstractButton) component;
            assertEquals(button.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                    FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            assertEquals(button.getClientProperty(FlatClientProperties.STYLE_CLASS), "inTextField");
            assertEquals(button.getPreferredSize(), new Dimension(
                    SearchTextField.OPTION_BUTTON_SIZE,
                    SearchTextField.OPTION_BUTTON_SIZE
            ));
            assertFalse(button.isFocusable());
            assertNotNull(button.getToolTipText());
        }
    }
}

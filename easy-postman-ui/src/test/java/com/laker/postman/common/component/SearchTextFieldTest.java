package com.laker.postman.common.component;

import org.testng.annotations.Test;

import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import static org.testng.Assert.assertFalse;
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
}

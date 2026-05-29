package com.laker.postman.common.component.button;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class ToolbarButtonTest {

    @Test
    public void shouldCreateThemedIconToolbarButtons() {
        EditButton editButton = new EditButton();

        assertNotNull(editButton.getIcon());
        assertEquals(editButton.getToolTipText(), "Edit");
        assertFalse(editButton.isFocusable());
    }

    @Test
    public void shouldKeepSelectedWrapIconOnPrimaryColor() {
        WrapToggleButton wrapButton = new WrapToggleButton();

        wrapButton.setSelected(true);

        assertNotNull(wrapButton.getIcon());
        assertEquals(wrapButton.getToolTipText(), "Toggle Line Wrap");
    }
}

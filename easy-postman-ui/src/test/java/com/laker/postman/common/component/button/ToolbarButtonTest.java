package com.laker.postman.common.component.button;

import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class ToolbarButtonTest {

    @Test
    public void shouldCreateThemedIconToolbarButtons() {
        EditButton editButton = new EditButton();

        assertNotNull(editButton.getIcon());
        assertEquals(editButton.getToolTipText(), CommonI18n.get(CommonMessageKeys.BUTTON_EDIT));
        assertFalse(editButton.isFocusable());
    }

    @Test
    public void shouldKeepSelectedWrapIconOnPrimaryColor() {
        WrapToggleButton wrapButton = new WrapToggleButton();

        wrapButton.setSelected(true);

        assertNotNull(wrapButton.getIcon());
        assertEquals(wrapButton.getToolTipText(), UiI18n.get(UiMessageKeys.BUTTON_TOGGLE_LINE_WRAP));
    }
}

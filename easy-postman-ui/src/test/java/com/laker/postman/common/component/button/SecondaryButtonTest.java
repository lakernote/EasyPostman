package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import org.testng.annotations.Test;

import javax.swing.JButton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class SecondaryButtonTest {

    @Test
    public void shouldDelegateChromeToFlatLafStyleClass() {
        JButton button = new SecondaryButton("Cancel");

        assertEquals(button.getClientProperty(FlatClientProperties.STYLE_CLASS),
                ModernButtonFactory.SECONDARY_STYLE_CLASS);
        assertNull(button.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                "SecondaryButton must not opt into FlatLaf pill-style roundRect buttons");
        assertTrue(button.isContentAreaFilled());
        assertTrue(button.isBorderPainted());
        assertTrue(button.isFocusPainted());
    }
}

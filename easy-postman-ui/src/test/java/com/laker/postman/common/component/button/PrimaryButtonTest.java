package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import org.testng.annotations.Test;

import javax.swing.JButton;
import java.awt.Color;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PrimaryButtonTest {

    @Test
    public void shouldDelegateChromeToFlatLafStyleClass() {
        JButton button = new PrimaryButton("Send");

        assertEquals(button.getClientProperty(FlatClientProperties.STYLE_CLASS),
                ModernButtonFactory.PRIMARY_STYLE_CLASS);
        assertNull(button.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                "PrimaryButton must not opt into FlatLaf pill-style roundRect buttons");
        assertTrue(button.isContentAreaFilled());
        assertTrue(button.isBorderPainted());
        assertTrue(button.isFocusPainted());
    }

    @Test
    public void shouldMapLegacyColorClientPropertiesToFlatLafStyle() {
        JButton button = new PrimaryButton("Run");

        button.putClientProperty("baseColor", new Color(12, 34, 56));
        button.putClientProperty("hoverColor", new Color(78, 90, 102));
        button.putClientProperty("pressColor", new Color(111, 122, 133));
        button.putClientProperty("colorsInitialized", false);

        String style = Objects.toString(button.getClientProperty(FlatClientProperties.STYLE), "");
        assertTrue(style.contains("focusWidth: 0"));
        assertTrue(style.contains("innerFocusWidth: 0"));
        assertTrue(style.contains("background: #0c2238"));
        assertTrue(style.contains("hoverBackground: #4e5a66"));
        assertTrue(style.contains("pressedBackground: #6f7a85"));
        assertTrue(style.contains("focusedBorderColor: #4e5a66"));
        assertTrue(style.contains("focusColor: #4e5a66"));
    }
}

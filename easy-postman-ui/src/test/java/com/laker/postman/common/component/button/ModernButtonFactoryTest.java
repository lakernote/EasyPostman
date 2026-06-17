package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import org.testng.annotations.Test;

import javax.swing.JButton;
import javax.swing.AbstractButton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ModernButtonFactoryTest {

    @Test
    public void buttonsShouldUseFlatLafStyleClassesWithoutPillButtonType() {
        JButton primaryButton = ModernButtonFactory.createButton("OK", true);
        JButton secondaryButton = ModernButtonFactory.createButton("Cancel", false);

        assertNull(primaryButton.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                "Modern buttons should use normal FlatLaf button painting, not pill-like roundRect type");
        assertEquals(primaryButton.getClientProperty(FlatClientProperties.STYLE_CLASS),
                ModernButtonFactory.PRIMARY_STYLE_CLASS);
        assertEquals(secondaryButton.getClientProperty(FlatClientProperties.STYLE_CLASS),
                ModernButtonFactory.SECONDARY_STYLE_CLASS);
        assertTrue(primaryButton.isContentAreaFilled(), "FlatLaf should paint the button background");
        assertTrue(primaryButton.isBorderPainted(), "FlatLaf should paint the button border");
        assertTrue(primaryButton.isFocusPainted(), "FlatLaf should keep keyboard focus visible");
    }

    @Test
    public void toggleButtonsShouldUseFlatLafStyleClassWithoutPillButtonType() {
        AbstractButton button = ModernButtonFactory.createToggleButton("Mode");

        assertNull(button.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                "Modern toggle buttons should use normal FlatLaf button painting, not pill-like roundRect type");
        assertEquals(button.getClientProperty(FlatClientProperties.STYLE_CLASS),
                ModernButtonFactory.TOGGLE_STYLE_CLASS);
        assertTrue(button.isContentAreaFilled(), "FlatLaf should paint the toggle background");
        assertTrue(button.isBorderPainted(), "FlatLaf should paint the toggle border");
    }

    @Test
    public void compactButtonsShouldUseSharedToolWindowSizing() {
        JButton button = ModernButtonFactory.createCompactButton("Save", false, "icons/save.svg");

        assertEquals(button.getPreferredSize().height, ModernButtonFactory.COMPACT_BUTTON_HEIGHT);
        assertEquals(button.getMinimumSize().height, ModernButtonFactory.COMPACT_BUTTON_HEIGHT);
        assertEquals(button.getMaximumSize().height, ModernButtonFactory.COMPACT_BUTTON_HEIGHT);
        assertTrue(button.getPreferredSize().width >= ModernButtonFactory.COMPACT_BUTTON_MIN_WIDTH);
        assertEquals(ModernButtonFactory.compactButtonWidth(64, 16, 6), 110);
        assertEquals(ModernButtonFactory.compactButtonWidth(20, 16, 6), 72);
    }
}

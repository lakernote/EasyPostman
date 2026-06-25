package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import org.testng.annotations.Test;

import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;

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

    @Test
    public void helpButtonShouldMatchIconToolbarSizing() {
        HelpButton button = new HelpButton();
        Dimension size = new Dimension(ToolWindowActionToolbar.ACTION_SIZE, ToolWindowActionToolbar.ACTION_SIZE);

        assertEquals(button.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        assertEquals(button.getPreferredSize(), size);
        assertEquals(button.getMinimumSize(), size);
        assertEquals(button.getMaximumSize(), size);
        assertEquals(button.getAlignmentY(), Component.CENTER_ALIGNMENT);
        assertEquals(button.getCursor().getType(), Cursor.HAND_CURSOR);
        Icon icon = button.getIcon();
        assertEquals(icon.getIconWidth(), 20);
        assertEquals(icon.getIconHeight(), 20);
    }
}

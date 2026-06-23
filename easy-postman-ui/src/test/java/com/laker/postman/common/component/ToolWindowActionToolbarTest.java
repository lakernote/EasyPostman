package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import org.testng.annotations.Test;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ToolWindowActionToolbarTest {

    @Test
    public void shouldUseConsistentInsetsAndButtonSizes() {
        JButton plusButton = new JButton();
        JButton refreshButton = new JButton();

        ToolWindowActionToolbar toolbar = ToolWindowActionToolbar.left(plusButton, refreshButton);

        assertTrue(toolbar.getBorder() instanceof EmptyBorder);
        assertEquals(toolbar.getInsets().top, ToolWindowActionToolbar.VERTICAL_PADDING);
        assertEquals(toolbar.getInsets().left, ToolWindowActionToolbar.HORIZONTAL_PADDING);
        assertEquals(toolbar.getInsets().bottom, ToolWindowActionToolbar.VERTICAL_PADDING);
        assertEquals(toolbar.getInsets().right, ToolWindowActionToolbar.HORIZONTAL_PADDING);
        assertSame(toolbar.getComponent(0), plusButton);
        assertTrue(toolbar.getComponent(1) instanceof Box.Filler);
        assertSame(toolbar.getComponent(2), refreshButton);
        assertEquals(plusButton.getPreferredSize(), new Dimension(
                ToolWindowActionToolbar.ACTION_SIZE,
                ToolWindowActionToolbar.ACTION_SIZE
        ));
        assertEquals(refreshButton.getPreferredSize(), new Dimension(
                ToolWindowActionToolbar.ACTION_SIZE,
                ToolWindowActionToolbar.ACTION_SIZE
        ));
        assertEquals(plusButton.getAlignmentY(), Component.CENTER_ALIGNMENT);
        assertEquals(refreshButton.getAlignmentY(), Component.CENTER_ALIGNMENT);
    }

    @Test
    public void shouldSupportRightAlignedActionRows() {
        JButton refreshButton = new JButton();

        ToolWindowActionToolbar toolbar = ToolWindowActionToolbar.right(refreshButton);

        assertTrue(toolbar.getComponent(0) instanceof Box.Filler);
        assertSame(toolbar.getComponent(1), refreshButton);
        assertEquals(refreshButton.getPreferredSize(), new Dimension(
                ToolWindowActionToolbar.ACTION_SIZE,
                ToolWindowActionToolbar.ACTION_SIZE
        ));
    }

    @Test
    public void shouldAllowInlineRowsWithoutExtraInsets() {
        JLabel label = new JLabel("Status");

        ToolWindowActionToolbar toolbar = ToolWindowActionToolbar.inlineLeft(label);

        assertEquals(toolbar.getInsets().top, 0);
        assertEquals(toolbar.getInsets().left, 0);
        assertSame(toolbar.getComponent(0), label);
        assertEquals(label.getAlignmentY(), Component.CENTER_ALIGNMENT);
    }

    @Test
    public void shouldPreserveTextButtonWidthAndCheckboxSize() {
        JButton textButton = new JButton("Save");
        Dimension textButtonWidth = textButton.getPreferredSize();
        JCheckBox checkBox = new JCheckBox("Enabled");
        Dimension checkBoxSize = checkBox.getPreferredSize();

        ToolWindowActionToolbar.inlineLeft(textButton, checkBox);

        assertEquals(textButton.getPreferredSize().width, Math.max(
                textButtonWidth.width,
                ToolWindowActionToolbar.ACTION_SIZE
        ));
        assertEquals(textButton.getPreferredSize().height, ToolWindowActionToolbar.ACTION_SIZE);
        assertEquals(checkBox.getPreferredSize(), checkBoxSize);
    }

    @Test
    public void shouldLeavePlainTextButtonChromeToThemeDefaults() {
        JButton textButton = new JButton("Format");

        ToolWindowActionToolbar.inlineLeft(textButton);

        assertNull(textButton.getClientProperty(FlatClientProperties.STYLE_CLASS));
        assertNull(textButton.getClientProperty(FlatClientProperties.STYLE));
        assertEquals(textButton.getPreferredSize().height, ToolWindowActionToolbar.ACTION_SIZE);
        assertTrue(textButton.isFocusPainted(), "keyboard focus visibility should remain enabled");
    }

    @Test
    public void shouldPreserveExplicitButtonChrome() {
        JButton toolbarButton = new JButton("Open");
        toolbarButton.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

        ToolWindowActionToolbar.inlineLeft(toolbarButton);

        assertEquals(toolbarButton.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        assertNull(toolbarButton.getClientProperty(FlatClientProperties.STYLE_CLASS));
    }
}

package com.laker.postman.common.component;

import com.laker.postman.common.component.button.PlusButton;
import com.laker.postman.common.component.button.RefreshButton;
import org.testng.annotations.Test;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ToolWindowActionToolbarTest {

    @Test
    public void shouldUseConsistentInsetsAndButtonSizes() {
        PlusButton plusButton = new PlusButton();
        RefreshButton refreshButton = new RefreshButton();

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
        RefreshButton refreshButton = new RefreshButton();

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
}

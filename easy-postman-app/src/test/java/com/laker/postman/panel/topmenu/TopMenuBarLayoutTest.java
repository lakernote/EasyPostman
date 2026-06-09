package com.laker.postman.panel.topmenu;

import org.testng.annotations.Test;

import javax.swing.border.EmptyBorder;
import java.awt.Insets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TopMenuBarLayoutTest {

    @Test
    public void topMenuBarShouldUsePaddingOnlyWithoutBottomDividerLine() {
        var border = TopMenuBar.createPanelBorder();

        assertTrue(border instanceof EmptyBorder);
        Insets insets = border.getBorderInsets(null);
        assertEquals(insets.top, 2);
        assertEquals(insets.left, 4);
        assertEquals(insets.bottom, 1);
        assertEquals(insets.right, 8);
    }
}

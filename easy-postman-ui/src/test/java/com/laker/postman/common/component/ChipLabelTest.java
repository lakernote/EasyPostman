package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import org.testng.annotations.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class ChipLabelTest {

    @Test
    public void shouldStoreAccentAndUseNonOpaquePainting() {
        Color accent = ModernColors.getInfo();
        ChipLabel label = new ChipLabel("Topic", accent);

        assertEquals(label.getText(), "Topic");
        assertEquals(label.getAccentColor(), accent);
        assertFalse(label.isOpaque());
        assertNotNull(label.getBorder());
        assertNotNull(label.getForeground());
    }

    @Test
    public void shouldUpdateTextAccentAndFallbackForeground() {
        ChipLabel label = new ChipLabel("Status", ModernColors.getSuccess());

        label.setChip("Plain", null);

        assertEquals(label.getText(), "Plain");
        assertNull(label.getAccentColor());
        assertEquals(label.getForeground(), ModernColors.getTextPrimary());
    }

    @Test
    public void shouldUseDisabledSemanticForeground() {
        ChipLabel label = new ChipLabel("Disabled", ModernColors.getWarningDark());

        label.setEnabled(false);

        assertEquals(label.getForeground(), ModernColors.getTextDisabled());
    }

    @Test
    public void shouldPaintAccentWithoutThrowing() {
        ChipLabel label = new ChipLabel("Paint", ModernColors.getAccent());
        label.setSize(80, 24);
        BufferedImage image = new BufferedImage(80, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            label.paint(g);
        } finally {
            g.dispose();
        }

        assertEquals(label.getWidth(), 80);
    }
}

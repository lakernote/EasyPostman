package com.laker.postman.util;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class FontManagerTest {

    private Font previousDefaultFont;
    private Font previousLabelFont;

    @BeforeMethod
    public void setUp() {
        previousDefaultFont = UIManager.getFont("defaultFont");
        previousLabelFont = UIManager.getFont("Label.font");
        UIManager.put("defaultFont", null);
        if (previousLabelFont == null) {
            UIManager.put("Label.font", new FontUIResource("Dialog", Font.PLAIN, 13));
        }
    }

    @AfterMethod
    public void tearDown() {
        UIManager.put("defaultFont", previousDefaultFont);
        UIManager.put("Label.font", previousLabelFont);
    }

    @Test
    public void shouldApplyFontAsFlatLafDefaultFont() {
        FontManager.applyFont("", 17);

        Font defaultFont = UIManager.getFont("defaultFont");

        assertNotNull(defaultFont);
        assertEquals(defaultFont.getSize(), 17);
    }

    @Test
    public void shouldInstallFontDefaultsWithoutWindowRefresh() {
        Font installedFont = FontManager.installFontDefaults("", 19);

        Font defaultFont = UIManager.getFont("defaultFont");

        assertNotNull(installedFont);
        assertNotNull(defaultFont);
        assertEquals(installedFont.getSize(), 19);
        assertEquals(defaultFont.getSize(), 19);
    }
}

package com.laker.postman.util;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Locale;

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

    @Test
    public void shouldRestoreCapturedLookAndFeelFontWhenSystemDefaultIsSelected() {
        UIManager.put("Label.font", new FontUIResource(Font.DIALOG, Font.PLAIN, 13));
        FontManager.captureLookAndFeelDefaultFont();

        UIManager.put("Label.font", new FontUIResource(Font.MONOSPACED, Font.PLAIN, 13));
        UIManager.put("defaultFont", new FontUIResource(Font.MONOSPACED, Font.PLAIN, 13));

        Font installedFont = FontManager.installFontDefaults("", 18);

        assertEquals(installedFont.getFamily(), Font.DIALOG);
        assertEquals(installedFont.getSize(), 18);
        assertEquals(UIManager.getFont("defaultFont").getFamily(), Font.DIALOG);
    }

    @Test
    public void shouldIgnoreSavedCjkUnsafeUiFontForChineseLocale() {
        assertEquals(
                FontManager.resolveAllowedUiFontName("Consolas", Locale.CHINESE, UiFontCatalog.FontSupport.NO_CJK),
                ""
        );
        assertEquals(
                FontManager.resolveAllowedUiFontName("Consolas", Locale.ENGLISH, UiFontCatalog.FontSupport.NO_CJK),
                "Consolas"
        );
    }
}

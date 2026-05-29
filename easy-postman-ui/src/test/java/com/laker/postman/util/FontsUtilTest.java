package com.laker.postman.util;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

import static org.testng.Assert.assertEquals;

public class FontsUtilTest {

    private Font previousDefaultFont;
    private Font previousLabelFont;

    @BeforeMethod
    public void setUp() {
        previousDefaultFont = UIManager.getFont("defaultFont");
        previousLabelFont = UIManager.getFont("Label.font");
        UIManager.put("defaultFont", new FontUIResource("Dialog", Font.PLAIN, 31));
        UIManager.put("Label.font", new FontUIResource("Dialog", Font.PLAIN, 13));
    }

    @AfterMethod
    public void tearDown() {
        UIManager.put("defaultFont", previousDefaultFont);
        UIManager.put("Label.font", previousLabelFont);
    }

    @Test
    public void shouldUseFlatLafDefaultFontAsBaseSize() {
        assertEquals(FontsUtil.getDefaultFont(Font.PLAIN).getSize(), 31);
    }
}

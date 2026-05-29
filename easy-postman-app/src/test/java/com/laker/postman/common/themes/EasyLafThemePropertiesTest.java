package com.laker.postman.common.themes;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class EasyLafThemePropertiesTest {

    private LookAndFeel previousLookAndFeel;

    @BeforeMethod
    public void setUp() {
        previousLookAndFeel = UIManager.getLookAndFeel();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (previousLookAndFeel != null) {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
    }

    @Test
    public void shouldDefineSharedSemanticColorDefaultsForBuiltInThemes() throws Exception {
        assertDefinesThemeColors("com/laker/postman/common/themes/EasyLightLaf.properties");
        assertDefinesThemeColors("com/laker/postman/common/themes/EasyDarkLaf.properties");
    }

    @Test
    public void shouldExposeSemanticColorsThroughUiManagerAfterLafSetup() {
        assertTrue(EasyLightLaf.setup());
        assertEquals(UIManager.getColor(ThemeColors.TEXT_PRIMARY), new Color(15, 23, 42));
        assertEquals(UIManager.getColor(ThemeColors.BACKGROUND), new Color(245, 247, 250));

        assertTrue(EasyDarkLaf.setup());
        assertEquals(UIManager.getColor(ThemeColors.TEXT_PRIMARY), new Color(241, 245, 249));
        assertEquals(UIManager.getColor(ThemeColors.BACKGROUND), new Color(60, 63, 65));

        assertNotNull(UIManager.getColor(ThemeColors.CONSOLE_SELECTION_BACKGROUND));
    }

    private void assertDefinesThemeColors(String resourcePath) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(in != null, "Missing theme properties: " + resourcePath);
            properties.load(in);
        }

        for (String key : ThemeColors.REQUIRED_KEYS) {
            assertTrue(properties.containsKey(key), resourcePath + " must define " + key);
        }
    }
}

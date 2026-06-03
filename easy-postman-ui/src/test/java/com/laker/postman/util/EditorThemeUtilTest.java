package com.laker.postman.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;

public class EditorThemeUtilTest {
    private LookAndFeel previousLookAndFeel;

    @BeforeMethod
    public void rememberLookAndFeel() {
        previousLookAndFeel = UIManager.getLookAndFeel();
        EditorThemeUtil.clearConfiguredThemeResources();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        EditorThemeUtil.clearConfiguredThemeResources();
        if (previousLookAndFeel != null) {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
    }

    @Test
    public void themeResourcePathShouldFollowInstalledFlatLafTheme() {
        FlatDarkLaf.setup();
        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/easypostman-dark.xml");

        FlatLightLaf.setup();
        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/easypostman-light.xml");
    }

    @Test
    public void configuredThemeResourceShouldOverrideFlatLafDetection() {
        FlatDarkLaf.setup();

        EditorThemeUtil.configureThemeResources("themes/custom-light.xml", "fallback.xml");

        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/custom-light.xml");
    }

    @Test
    public void shouldFallBackToFlatLafDetectionWhenConfiguredPathIsBlank() {
        FlatLightLaf.setup();

        EditorThemeUtil.configureThemeResources(" ", " ");

        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/easypostman-light.xml");
    }
}

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
    }

    @AfterMethod
    public void tearDown() throws Exception {
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
}

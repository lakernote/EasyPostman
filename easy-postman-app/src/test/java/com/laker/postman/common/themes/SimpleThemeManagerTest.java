package com.laker.postman.common.themes;

import com.formdev.flatlaf.FlatDarkLaf;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertTrue;

public class SimpleThemeManagerTest {

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
    public void shouldReportDarkModeFromInstalledFlatLaf() throws Exception {
        UIManager.setLookAndFeel(new FlatDarkLaf());

        assertTrue(SimpleThemeManager.isDarkTheme());
    }
}

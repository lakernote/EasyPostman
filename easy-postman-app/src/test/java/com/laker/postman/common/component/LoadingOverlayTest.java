package com.laker.postman.common.component;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class LoadingOverlayTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.BACKGROUND, ThemeColors.BORDER_MEDIUM);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void overlayColorShouldUseSemanticBackgroundWithOverlayAlpha() {
        Color background = new Color(11, 12, 13);
        UIManager.put(ThemeColors.BACKGROUND, background);

        Color overlay = LoadingOverlayTheme.overlay();

        assertEquals(overlay, new Color(background.getRed(), background.getGreen(), background.getBlue(), 230));
    }

    @Test
    public void spinnerBackgroundShouldUseSemanticBorderMediumColor() {
        Color spinnerBackground = new Color(31, 32, 33);
        UIManager.put(ThemeColors.BORDER_MEDIUM, spinnerBackground);

        Color actual = LoadingOverlayTheme.spinnerBackground();

        assertEquals(actual, spinnerBackground);
    }
}

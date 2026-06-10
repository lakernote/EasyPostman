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
        previousThemeTokens = remember(
                ThemeColors.BACKGROUND,
                ThemeColors.PRIMARY
        );
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

        assertEquals(overlay, new Color(background.getRed(), background.getGreen(), background.getBlue(), 124));
    }

    @Test
    public void activeSpinnerSegmentShouldUseSemanticPrimaryColor() {
        Color primary = new Color(31, 75, 199);
        UIManager.put(ThemeColors.PRIMARY, primary);

        Color actual = LoadingOverlayTheme.spinnerSegment(0, 12);

        assertEquals(actual, new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 205));
    }

    @Test
    public void fadedSpinnerSegmentShouldUseLowerAlphaThanActiveSegment() {
        Color primary = new Color(31, 75, 199);
        UIManager.put(ThemeColors.PRIMARY, primary);

        Color active = LoadingOverlayTheme.spinnerSegment(0, 12);
        Color faded = LoadingOverlayTheme.spinnerSegment(11, 12);

        assertEquals(active.getAlpha(), 205);
        assertEquals(faded.getAlpha(), 36);
    }
}

package com.laker.postman.common.component.tab;

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

public class PlusPanelThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.PRIMARY, ThemeColors.ACCENT, ThemeColors.TEXT_PRIMARY);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void hoverGradientShouldReadCurrentThemeTokens() {
        Color primary = new Color(11, 21, 31);
        Color accent = new Color(41, 51, 61);
        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.ACCENT, accent);

        GradientPaint paint = PlusPanelTheme.hoverGradient(120);

        assertEquals(paint.getColor1(), primary);
        assertEquals(paint.getColor2(), accent);
    }

    @Test
    public void hintForegroundShouldUseReadableThemeText() {
        Color textPrimary = new Color(211, 212, 213);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);

        assertEquals(PlusPanelTheme.hintForeground(), textPrimary);
    }
}

package com.laker.postman.panel.collections.editor;

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

public class RequestEditorEmptyStateThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.PRIMARY,
                ThemeColors.ACCENT,
                ThemeColors.SURFACE,
                ThemeColors.BORDER_LIGHT,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_HINT,
                ThemeColors.HOVER_BACKGROUND
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void cardColorsShouldReadCurrentThemeTokens() {
        Color surface = new Color(21, 22, 23);
        Color borderLight = new Color(51, 52, 53);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.BORDER_LIGHT, borderLight);

        assertEquals(RequestEditorEmptyStateTheme.cardBackground(), surface);
        assertEquals(RequestEditorEmptyStateTheme.cardBorder(), borderLight);
        assertEquals(RequestEditorEmptyStateTheme.hintBorder(), borderLight);
    }

    @Test
    public void accentGradientsShouldReadCurrentThemeTokens() {
        Color primary = new Color(11, 21, 31);
        Color accent = new Color(41, 51, 61);
        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.ACCENT, accent);

        GradientPaint topAccent = RequestEditorEmptyStateTheme.topAccentGradient(120);
        GradientPaint hintHover = RequestEditorEmptyStateTheme.hintHoverGradient(120);

        assertEquals(topAccent.getColor1(), primary);
        assertEquals(topAccent.getColor2(), accent);
        assertEquals(hintHover.getColor1(), primary);
        assertEquals(hintHover.getColor2(), accent);
    }

    @Test
    public void logoColorsShouldReadCurrentPrimaryToken() {
        Color primary = new Color(11, 21, 31);
        UIManager.put(ThemeColors.PRIMARY, primary);

        assertColorWithAlpha(RequestEditorEmptyStateTheme.logoOuterGlow(), primary, 12);
        assertColorWithAlpha(RequestEditorEmptyStateTheme.logoMiddleGlow(), primary, 20);
        assertColorWithAlpha(RequestEditorEmptyStateTheme.logoBackground(), primary, 30);
        assertColorWithAlpha(RequestEditorEmptyStateTheme.logoBorder(), primary, 60);
    }

    @Test
    public void textColorsShouldReadCurrentThemeTokens() {
        Color textPrimary = new Color(211, 212, 213);
        Color textHint = new Color(141, 142, 143);
        Color hoverBackground = new Color(61, 62, 63);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);
        UIManager.put(ThemeColors.TEXT_HINT, textHint);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, hoverBackground);

        assertEquals(RequestEditorEmptyStateTheme.titleForeground(), textPrimary);
        assertEquals(RequestEditorEmptyStateTheme.hintForeground(), textPrimary);
        assertEquals(RequestEditorEmptyStateTheme.shortcutForeground(), textHint);
        assertEquals(RequestEditorEmptyStateTheme.hintBackground(), hoverBackground);
        assertEquals(RequestEditorEmptyStateTheme.hintHoverForeground(), Color.WHITE);
    }

    private void assertColorWithAlpha(Color actual, Color expectedRgb, int expectedAlpha) {
        assertEquals(actual.getRed(), expectedRgb.getRed());
        assertEquals(actual.getGreen(), expectedRgb.getGreen());
        assertEquals(actual.getBlue(), expectedRgb.getBlue());
        assertEquals(actual.getAlpha(), expectedAlpha);
    }
}

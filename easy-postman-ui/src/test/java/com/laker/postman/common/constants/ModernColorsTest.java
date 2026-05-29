package com.laker.postman.common.constants;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class ModernColorsTest {
    private static final List<String> THEME_TOKEN_KEYS = List.of(
            ThemeColors.PRIMARY,
            ThemeColors.PRIMARY_DARK,
            ThemeColors.PRIMARY_DARKER,
            ThemeColors.SUCCESS,
            ThemeColors.SELECTION_BACKGROUND,
            ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND,
            ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND,
            ThemeColors.SPLASH_GRADIENT_START,
            ThemeColors.SPLASH_GRADIENT_END
    );

    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = new HashMap<>();
        for (String key : THEME_TOKEN_KEYS) {
            previousThemeTokens.put(key, UIManager.get(key));
        }
    }

    @AfterMethod
    public void tearDown() {
        for (Map.Entry<String, Object> entry : previousThemeTokens.entrySet()) {
            UIManager.put(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void shouldReadBrandAndStatusColorsFromUiDefaults() {
        Color primary = new Color(1, 2, 3);
        Color success = new Color(4, 5, 6);

        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.SUCCESS, success);

        assertEquals(ModernColors.getPrimary(), primary);
        assertEquals(ModernColors.getSuccess(), success);
    }

    @Test
    public void shouldCreateAlphaColorsFromCurrentThemeTokens() {
        UIManager.put(ThemeColors.PRIMARY, new Color(10, 20, 30));

        assertEquals(ModernColors.primaryWithAlpha(77), new Color(10, 20, 30, 77));
    }

    @Test
    public void shouldApplyAlphaToAnyColor() {
        assertEquals(ModernColors.withAlpha(new Color(1, 2, 3), 44), new Color(1, 2, 3, 44));
    }

    @Test
    public void shouldReadSelectionAndSearchHighlightColorsFromUiDefaults() {
        Color selection = new Color(21, 22, 23);
        Color highlight = new Color(31, 32, 33);
        Color currentHighlight = new Color(41, 42, 43);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selection);
        UIManager.put(ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND, highlight);
        UIManager.put(ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND, currentHighlight);

        assertEquals(ModernColors.getSelectionBackgroundColor(), selection);
        assertEquals(ModernColors.getSearchHighlightBackgroundColor(), highlight);
        assertEquals(ModernColors.getSearchCurrentHighlightBackgroundColor(), currentHighlight);
    }

    @Test
    public void shouldReadSplashGradientColorsFromUiDefaults() {
        Color start = new Color(51, 52, 53);
        Color end = new Color(61, 62, 63);
        UIManager.put(ThemeColors.SPLASH_GRADIENT_START, start);
        UIManager.put(ThemeColors.SPLASH_GRADIENT_END, end);

        assertEquals(ModernColors.getSplashGradientStartColor(), start);
        assertEquals(ModernColors.getSplashGradientEndColor(), end);
    }
}

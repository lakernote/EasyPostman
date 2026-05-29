package com.laker.postman.panel.history;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class HistoryPanelThemeTest {
    private static final List<String> THEME_TOKEN_KEYS = List.of(
            ThemeColors.SELECTION_BACKGROUND,
            ThemeColors.HOVER_BACKGROUND,
            ThemeColors.TEXT_PRIMARY,
            ThemeColors.TEXT_SECONDARY,
            ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND
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
    public void restoreThemeTokens() {
        for (Map.Entry<String, Object> entry : previousThemeTokens.entrySet()) {
            UIManager.put(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void shouldUseThemeTokensForHistoryListSurfaces() {
        Color selection = new Color(44, 45, 46);
        Color hover = new Color(47, 48, 49);
        Color textPrimary = new Color(210, 211, 212);
        Color textSecondary = new Color(180, 181, 182);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selection);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, hover);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);
        UIManager.put(ThemeColors.TEXT_SECONDARY, textSecondary);

        assertEquals(HistoryTheme.selectionBackground(), selection);
        assertEquals(HistoryTheme.hoverBackground(), hover);
        assertEquals(HistoryTheme.selectedTitleForeground(), textPrimary);
        assertEquals(HistoryTheme.groupHeaderForeground(), textSecondary);
    }

    @Test
    public void shouldUseThemeTokenForSearchHighlightHtml() {
        UIManager.put(ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND, new Color(170, 187, 204));

        assertEquals(HistoryTheme.searchHighlightBackgroundHex(), "#aabbcc");
    }
}

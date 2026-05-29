package com.laker.postman.panel.sidebar;

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

public class ConsolePanelThemeTest {
    private static final List<String> THEME_TOKEN_KEYS = List.of(
            ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND,
            ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND
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
    public void shouldUseSearchHighlightThemeTokens() {
        Color highlight = new Color(80, 81, 82);
        Color currentHighlight = new Color(90, 91, 92);
        UIManager.put(ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND, highlight);
        UIManager.put(ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND, currentHighlight);

        assertEquals(ConsoleTheme.searchHighlightBackground(), highlight);
        assertEquals(ConsoleTheme.searchCurrentHighlightBackground(), currentHighlight);
    }
}

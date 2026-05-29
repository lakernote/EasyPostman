package com.laker.postman.panel.topmenu.plugin;

import com.laker.postman.common.constants.ModernColors;
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

public class PluginManagerDialogThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.SELECTION_BACKGROUND, ThemeColors.WARNING, ThemeColors.TEXT_PRIMARY);
    }

    @AfterMethod
    public void restoreThemeTokens() {
        restore(previousThemeTokens);
    }

    @Test
    public void listSelectionBackgroundShouldUseSemanticSelectionToken() {
        Color selection = new Color(41, 42, 43);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selection);

        assertEquals(PluginManagerTheme.listSelectionBackground(), selection);
    }

    @Test
    public void statusPaletteShouldUseReadableForegroundOverStatusTint() {
        Color warning = new Color(201, 121, 31);
        Color textPrimary = new Color(30, 31, 32);
        UIManager.put(ThemeColors.WARNING, warning);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);

        assertEquals(PluginManagerTheme.statusBackground(warning), ModernColors.withAlpha(warning, 64));
        assertEquals(PluginManagerTheme.statusForeground(), textPrimary);
    }
}

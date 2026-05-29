package com.laker.postman.panel.toolbox;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;

import static org.testng.Assert.assertEquals;

public class ToolboxPanelThemeTest {
    private Object previousSelectionBackground;
    private Object previousTextPrimary;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousSelectionBackground = UIManager.get(ThemeColors.SELECTION_BACKGROUND);
        previousTextPrimary = UIManager.get(ThemeColors.TEXT_PRIMARY);
    }

    @AfterMethod
    public void restoreThemeTokens() {
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, previousSelectionBackground);
        UIManager.put(ThemeColors.TEXT_PRIMARY, previousTextPrimary);
    }

    @Test
    public void shouldUseThemeTokenForSelectedNavigationItemBackground() {
        Color selection = new Color(12, 34, 56);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selection);

        assertEquals(ToolboxTheme.selectedNavItemBackground(), selection);
    }

    @Test
    public void selectedNavigationItemTextShouldUseReadableThemeForeground() {
        Color textPrimary = new Color(220, 230, 240);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);

        assertEquals(ToolboxTheme.selectedNavItemForeground(), textPrimary);
    }
}

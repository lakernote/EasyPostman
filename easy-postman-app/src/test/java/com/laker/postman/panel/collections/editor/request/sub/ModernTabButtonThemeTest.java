package com.laker.postman.panel.collections.editor.request.sub;

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

public class ModernTabButtonThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                "TabbedPane.hoverColor",
                "TabbedPane.selectedBackground",
                ThemeColors.HOVER_BACKGROUND
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void fallbackHoverAndSelectedBackgroundShouldUseSemanticHoverToken() {
        Color hover = new Color(21, 22, 23);
        UIManager.put("TabbedPane.hoverColor", null);
        UIManager.put("TabbedPane.selectedBackground", null);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, hover);

        assertEquals(ModernTabButtonTheme.hoverBackground(0.5f), ModernColors.withAlpha(hover, 128));
        assertEquals(ModernTabButtonTheme.selectedBackground(), hover);
    }
}

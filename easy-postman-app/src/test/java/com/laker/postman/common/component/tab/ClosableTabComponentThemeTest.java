package com.laker.postman.common.component.tab;

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

public class ClosableTabComponentThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.PRIMARY,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_SECONDARY,
                ThemeColors.ERROR,
                ThemeColors.WARNING);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void closeButtonForegroundShouldUseSemanticTextSecondaryColor() {
        Color textSecondary = new Color(61, 62, 63);
        UIManager.put(ThemeColors.TEXT_SECONDARY, textSecondary);

        assertEquals(ClosableTabComponent.closeButtonForegroundColor(), textSecondary);
    }

    @Test
    public void closeButtonHoverForegroundShouldUseSemanticTextPrimaryColor() {
        Color textPrimary = new Color(21, 22, 23);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);

        assertEquals(ClosableTabComponent.closeButtonHoverForegroundColor(), textPrimary);
    }

    @Test
    public void dirtyDotShouldUseSemanticErrorColor() {
        Color error = new Color(231, 32, 33);
        UIManager.put(ThemeColors.ERROR, error);

        assertEquals(ClosableTabComponent.dirtyDotColor(), ModernColors.withAlpha(error, 180));
    }

    @Test
    public void newRequestDotShouldUseSemanticWarningColor() {
        Color warning = new Color(251, 191, 36);
        UIManager.put(ThemeColors.WARNING, warning);

        assertEquals(ClosableTabComponent.newRequestDotColor(), ModernColors.withAlpha(warning, 190));
    }

    @Test
    public void selectedTabBorderShouldUseSemanticPrimaryColor() {
        Color primary = new Color(0, 122, 255);
        UIManager.put(ThemeColors.PRIMARY, primary);

        assertEquals(ClosableTabComponent.selectedTabBorderColor(), ModernColors.withAlpha(primary, 130));
    }
}

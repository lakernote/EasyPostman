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
        previousThemeTokens = remember(ThemeColors.TEXT_PRIMARY, ThemeColors.ERROR);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void closeButtonForegroundShouldUseSemanticTextPrimaryColor() {
        Color textPrimary = new Color(21, 22, 23);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);

        assertEquals(ClosableTabComponent.closeButtonForegroundColor(), textPrimary);
    }

    @Test
    public void dirtyDotShouldUseSemanticErrorColor() {
        Color error = new Color(231, 32, 33);
        UIManager.put(ThemeColors.ERROR, error);

        assertEquals(ClosableTabComponent.dirtyDotColor(), ModernColors.withAlpha(error, 180));
    }
}

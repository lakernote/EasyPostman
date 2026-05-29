package com.laker.postman.common.component.placeholder;

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

public class PerformanceTrendPlaceholderThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.SURFACE);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void rootBackgroundShouldUseSemanticSurfaceToken() {
        Color surface = new Color(23, 24, 25);
        UIManager.put(ThemeColors.SURFACE, surface);

        PerformanceTrendPlaceholderPanel panel = new PerformanceTrendPlaceholderPanel();

        assertEquals(panel.getBackground(), surface);
    }
}

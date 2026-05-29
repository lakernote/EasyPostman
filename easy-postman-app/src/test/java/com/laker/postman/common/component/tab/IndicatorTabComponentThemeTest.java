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

public class IndicatorTabComponentThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.SUCCESS, ThemeColors.SUCCESS_DARK);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void indicatorColorsShouldUseSuccessThemeTokens() {
        Color success = new Color(21, 122, 23);
        Color successBorder = new Color(11, 92, 12);
        UIManager.put(ThemeColors.SUCCESS, success);
        UIManager.put(ThemeColors.SUCCESS_DARK, successBorder);

        assertEquals(IndicatorTabTheme.indicator(), success);
        assertEquals(IndicatorTabTheme.indicatorBorder(), ModernColors.withAlpha(successBorder, 100));
    }
}

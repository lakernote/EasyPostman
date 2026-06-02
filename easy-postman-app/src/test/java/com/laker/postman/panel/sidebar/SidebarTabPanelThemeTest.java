package com.laker.postman.panel.sidebar;

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

public class SidebarTabPanelThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.PRIMARY, ThemeColors.PRIMARY_LIGHT);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void selectedTabBackgroundShouldReadCurrentPrimaryToken() {
        Color primary = new Color(12, 34, 56);
        UIManager.put(ThemeColors.PRIMARY, primary);

        assertEquals(SidebarTheme.selectedTabBackground(), ModernColors.withAlpha(primary, 25));

        Color changedPrimary = new Color(72, 82, 92);
        UIManager.put(ThemeColors.PRIMARY, changedPrimary);

        assertEquals(SidebarTheme.selectedTabBackground(), ModernColors.withAlpha(changedPrimary, 25));
    }

    @Test
    public void selectedTabIndicatorShouldReadCurrentThemeTokens() {
        Color primary = new Color(13, 14, 15);
        Color primaryLight = new Color(23, 24, 25);
        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.PRIMARY_LIGHT, primaryLight);

        GradientPaint paint = SidebarTheme.selectedTabIndicatorPaint(42);

        assertEquals(paint.getColor1(), primary);
        assertEquals(paint.getColor2(), primaryLight);
    }

    @Test
    public void selectedTabTitleShouldUsePrimaryThemeColor() {
        Color primary = new Color(0, 122, 255);
        UIManager.put(ThemeColors.PRIMARY, primary);

        assertEquals(SidebarTheme.selectedTabTitleForeground(), primary);
    }
}

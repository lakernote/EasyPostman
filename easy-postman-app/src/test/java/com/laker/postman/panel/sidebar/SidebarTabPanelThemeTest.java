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
        previousThemeTokens = remember(ThemeColors.PRIMARY, ThemeColors.TEXT_SECONDARY);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void selectedTabBackgroundShouldUsePrimaryToken() {
        Color primary = new Color(12, 34, 56);
        UIManager.put(ThemeColors.PRIMARY, primary);
        int expandedAlpha = ModernColors.isDarkTheme() ? 36 : 22;

        assertEquals(SidebarTheme.selectedCollapsedTabBackground(), primary);
        assertEquals(SidebarTheme.selectedExpandedTabBackground(), ModernColors.withAlpha(primary, expandedAlpha));
    }

    @Test
    public void selectedTabTitleShouldUsePrimaryToken() {
        Color primary = new Color(72, 82, 92);
        UIManager.put(ThemeColors.PRIMARY, primary);

        assertEquals(SidebarTheme.selectedTabTitleForeground(), primary);
    }

    @Test
    public void inactiveTabChromeShouldUseThemeSecondaryTextColor() {
        Color secondaryText = new Color(82, 92, 102);
        UIManager.put(ThemeColors.TEXT_SECONDARY, secondaryText);

        assertEquals(SidebarTheme.inactiveTabIconForeground(), secondaryText);
        assertEquals(SidebarTheme.inactiveTabTitleForeground(), secondaryText);
    }
}

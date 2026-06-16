package com.laker.postman.panel.sidebar;

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
        previousThemeTokens = remember(
                ThemeColors.PRIMARY,
                ThemeColors.SELECTION_BACKGROUND,
                ThemeColors.TAB_HOVER_BACKGROUND,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_SECONDARY
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void selectedTabBackgroundShouldUsePrimaryToken() {
        Color primary = new Color(12, 34, 56);
        Color selectionBackground = new Color(212, 227, 255);
        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selectionBackground);

        assertEquals(SidebarTheme.selectedCollapsedTabBackground(), primary);
        assertEquals(SidebarTheme.selectedExpandedTabBackground(), selectionBackground);
    }

    @Test
    public void hoverTabBackgroundShouldUseSharedTabHoverToken() {
        Color hoverTabBackground = new Color(242, 246, 255);
        UIManager.put(ThemeColors.TAB_HOVER_BACKGROUND, hoverTabBackground);

        assertEquals(SidebarTheme.hoverTabBackground(), hoverTabBackground);
    }

    @Test
    public void selectedExpandedTabTitleShouldUsePrimaryTextToken() {
        Color primaryText = new Color(15, 23, 42);
        UIManager.put(ThemeColors.TEXT_PRIMARY, primaryText);

        assertEquals(SidebarTheme.selectedTabTitleForeground(), primaryText);
    }

    @Test
    public void inactiveTabChromeShouldUseThemeSecondaryTextColor() {
        Color secondaryText = new Color(82, 92, 102);
        UIManager.put(ThemeColors.TEXT_SECONDARY, secondaryText);

        assertEquals(SidebarTheme.inactiveTabIconForeground(), secondaryText);
        assertEquals(SidebarTheme.inactiveTabTitleForeground(), secondaryText);
    }
}

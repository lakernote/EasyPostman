package com.laker.postman.panel.sidebar;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class SidebarTheme {
    Color railBackground() {
        return ModernColors.getBackgroundColor();
    }

    Color hoverTabBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color selectedExpandedTabBackground() {
        return ModernColors.primaryWithAlpha(ModernColors.isDarkTheme() ? 36 : 22);
    }

    Color selectedCollapsedTabBackground() {
        return ModernColors.getPrimary();
    }

    Color selectedTabTitleForeground() {
        return ModernColors.getPrimary();
    }

    Color inactiveTabTitleForeground() {
        return ModernColors.getTextSecondary();
    }

    Color inactiveTabIconForeground() {
        return ModernColors.getTextSecondary();
    }
}

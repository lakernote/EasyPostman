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
        return ModernColors.getTabHoverBackgroundColor();
    }

    Color selectedExpandedTabBackground() {
        return ModernColors.getSelectionBackgroundColor();
    }

    Color selectedCollapsedTabBackground() {
        return ModernColors.getPrimary();
    }

    Color selectedTabTitleForeground() {
        return ModernColors.getTextPrimary();
    }

    Color inactiveTabTitleForeground() {
        return ModernColors.getTextSecondary();
    }

    Color inactiveTabIconForeground() {
        return ModernColors.getTextSecondary();
    }
}

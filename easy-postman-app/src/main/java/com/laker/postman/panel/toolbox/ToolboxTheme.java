package com.laker.postman.panel.toolbox;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class ToolboxTheme {
    Color selectedNavItemBackground() {
        return ModernColors.getSelectionBackgroundColor();
    }

    Color selectedNavItemForeground() {
        return ModernColors.getTextPrimary();
    }
}

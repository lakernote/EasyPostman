package com.laker.postman.panel.topmenu.plugin;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class PluginManagerTheme {
    Color listSelectionBackground() {
        return ModernColors.getSelectionBackgroundColor();
    }

    Color statusBackground(Color color) {
        return ModernColors.withAlpha(color, 64);
    }

    Color statusForeground() {
        return ModernColors.getTextPrimary();
    }
}

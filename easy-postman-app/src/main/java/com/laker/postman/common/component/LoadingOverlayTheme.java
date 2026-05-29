package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class LoadingOverlayTheme {
    Color overlay() {
        return ModernColors.withAlpha(ModernColors.getBackgroundColor(), 230);
    }

    Color spinner() {
        return ModernColors.getPrimary();
    }

    Color spinnerBackground() {
        return ModernColors.getBorderMediumColor();
    }

    Color messageForeground() {
        return ModernColors.getTextSecondary();
    }
}

package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class LoadingOverlayTheme {
    Color overlay() {
        return ModernColors.withAlpha(
                ModernColors.getBackgroundColor(),
                ModernColors.isDarkTheme() ? 152 : 124
        );
    }

    Color spinner() {
        return ModernColors.getPrimary();
    }

    Color spinnerSegment(int fadeIndex, int segmentCount) {
        int maxAlpha = ModernColors.isDarkTheme() ? 220 : 205;
        int minAlpha = ModernColors.isDarkTheme() ? 50 : 36;
        float progress = segmentCount <= 1
                ? 1.0f
                : 1.0f - Math.min(Math.max(fadeIndex, 0), segmentCount - 1) / (segmentCount - 1.0f);
        int alpha = minAlpha + Math.round((maxAlpha - minAlpha) * progress * progress);
        return ModernColors.withAlpha(spinner(), alpha);
    }
}

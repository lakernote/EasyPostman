package com.laker.postman.common.component.tab;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.awt.GradientPaint;

@UtilityClass
class PlusPanelTheme {
    GradientPaint hoverGradient(int width) {
        return new GradientPaint(
                0, 0, ModernColors.getPrimary(),
                width, 0, ModernColors.getAccent()
        );
    }

    GradientPaint hoverHighlight(int height) {
        return new GradientPaint(
                0, 0, ModernColors.whiteWithAlpha(30),
                0, height / 2.5f, ModernColors.whiteWithAlpha(0)
        );
    }

    Color hintForeground() {
        return ModernColors.getTextPrimary();
    }
}

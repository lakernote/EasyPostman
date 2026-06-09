package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.awt.GradientPaint;

@UtilityClass
class RequestEditorEmptyStateTheme {
    Color cardBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    Color cardBorder() {
        return ModernColors.getBorderLightColor();
    }

    Color cardShadow(int alpha) {
        return ModernColors.getShadowColor(alpha);
    }

    GradientPaint topAccentGradient(int width) {
        return new GradientPaint(0, 0, ModernColors.getPrimary(), width, 0, ModernColors.getAccent());
    }

    Color logoOuterGlow() {
        return ModernColors.primaryWithAlpha(12);
    }

    Color logoMiddleGlow() {
        return ModernColors.primaryWithAlpha(20);
    }

    Color logoBackground() {
        return ModernColors.primaryWithAlpha(30);
    }

    Color logoBorder() {
        return ModernColors.primaryWithAlpha(60);
    }

    Color titleForeground() {
        return ModernColors.getTextPrimary();
    }

    Color hintForeground() {
        return ModernColors.getTextPrimary();
    }

    Color hintHoverForeground() {
        return ModernColors.getTextInverse();
    }

    Color hintBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color hintBorder() {
        return ModernColors.getBorderLightColor();
    }

    GradientPaint hintHoverGradient(int width) {
        return new GradientPaint(0, 0, ModernColors.getPrimary(), width, 0, ModernColors.getAccent());
    }

    GradientPaint hintHoverHighlight(int height) {
        return new GradientPaint(0, 0, ModernColors.whiteWithAlpha(30), 0, height / 2.5f,
                ModernColors.whiteWithAlpha(0));
    }

    Color shortcutForeground() {
        return ModernColors.getTextHint();
    }
}

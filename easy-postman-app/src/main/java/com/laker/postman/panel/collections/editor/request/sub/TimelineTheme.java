package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class TimelineTheme {
    Color panelBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    Color infoBackground() {
        return sectionBackground();
    }

    Color infoBorder() {
        return ModernColors.getBorderLightColor();
    }

    Color barAreaBackground() {
        return sectionBackground();
    }

    Color barTrackBackground() {
        Color base = ModernColors.isDarkTheme()
                ? ModernColors.getBackgroundColor()
                : ModernColors.getCardBackgroundColor();
        Color border = ModernColors.getDividerBorderColor();
        return ModernColors.blendColors(base, border, ModernColors.isDarkTheme() ? 0.36f : 0.28f);
    }

    Color barTrackBorder() {
        return ModernColors.withAlpha(ModernColors.getDividerBorderColor(), ModernColors.isDarkTheme() ? 170 : 145);
    }

    Color gridLine() {
        return ModernColors.withAlpha(ModernColors.getDividerBorderColor(), ModernColors.isDarkTheme() ? 95 : 105);
    }

    Color labelText() {
        return ModernColors.getTextPrimary();
    }

    Color infoText() {
        return ModernColors.getTextSecondary();
    }

    Color descriptionText() {
        return ModernColors.getTextHint();
    }

    Color separator() {
        return ModernColors.getDividerBorderColor();
    }

    Color certificateWarning() {
        return ModernColors.getError();
    }

    Color[] barColors() {
        return new Color[]{
                ModernColors.getPrimary(),
                ModernColors.getSecondary(),
                ModernColors.getAccent(),
                ModernColors.getWarning(),
                ModernColors.getError(),
                ModernColors.getSuccess()
        };
    }

    Color hoveredBarBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color hoveredLabelText() {
        return ModernColors.getTextPrimary();
    }

    Color barShadow() {
        return ModernColors.getShadowColor(ModernColors.isDarkTheme() ? 8 : 12);
    }

    Color hoveredBarOutline() {
        return ModernColors.withAlpha(ModernColors.getPrimary(), ModernColors.isDarkTheme() ? 130 : 105);
    }

    Color barHighlight(boolean hovered) {
        int alpha = ModernColors.isDarkTheme()
                ? (hovered ? 34 : 24)
                : (hovered ? 58 : 42);
        return ModernColors.whiteWithAlpha(alpha);
    }

    Color barText() {
        return ModernColors.getTextInverse();
    }

    Color zeroDurationBar() {
        return ModernColors.getBorderMediumColor();
    }

    private Color sectionBackground() {
        return ModernColors.blendColors(
                ModernColors.getCardBackgroundColor(),
                ModernColors.getBackgroundColor(),
                ModernColors.isDarkTheme() ? 0.45f : 0.35f
        );
    }
}

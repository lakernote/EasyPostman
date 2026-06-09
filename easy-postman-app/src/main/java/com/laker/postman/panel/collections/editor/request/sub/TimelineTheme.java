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
        return ModernColors.getBackgroundColor();
    }

    Color infoBorder() {
        return ModernColors.getBorderLightColor();
    }

    Color barAreaBackground() {
        return ModernColors.getBackgroundColor();
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
        return ModernColors.getShadowColor(10);
    }

    Color hoveredBarOutline() {
        return ModernColors.withAlpha(ModernColors.getTextInverse(), 80);
    }
}

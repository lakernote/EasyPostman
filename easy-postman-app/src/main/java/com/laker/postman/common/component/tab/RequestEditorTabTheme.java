package com.laker.postman.common.component.tab;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.*;

@UtilityClass
public class RequestEditorTabTheme {

    public Color tabAreaBackground() {
        return ModernColors.getBackgroundColor();
    }

    public Color selectedTabBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    public Color hoverTabBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    public Color hoverTabBorder() {
        return ModernColors.getBorderLightColor();
    }

    public Color titleForeground(boolean selected) {
        return selected ? ModernColors.getTextPrimary() : ModernColors.getTextSecondary();
    }

    public Color closeButtonForeground() {
        return ModernColors.getTextSecondary();
    }

    public Color closeButtonHoverForeground() {
        return ModernColors.getTextPrimary();
    }

    public Color closeButtonHoverBackground() {
        return ModernColors.withAlpha(ModernColors.getHoverBackgroundColor(), 180);
    }

    public Color dirtyDot() {
        return ModernColors.withAlpha(ModernColors.getError(), 180);
    }

    public Color newRequestDot() {
        return ModernColors.withAlpha(ModernColors.getWarning(), 190);
    }
}

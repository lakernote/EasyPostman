package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import javax.swing.UIManager;
import java.awt.Color;

@UtilityClass
class ModernTabButtonTheme {
    Color foreground(boolean enabled) {
        return enabled ? ModernColors.getTextPrimary() : ModernColors.getTextHint();
    }

    Color indicator(boolean enabled) {
        Color color;
        if (!enabled) {
            color = UIManager.getColor("TabbedPane.disabledUnderlineColor");
            if (color != null) {
                return color;
            }

            color = UIManager.getColor("TabbedPane.disabledForeground");
            if (color != null) {
                return color;
            }
        } else {
            color = UIManager.getColor("TabbedPane.underlineColor");
            if (color != null) {
                return color;
            }
        }

        color = UIManager.getColor("Component.accentColor");
        return color != null ? color : ModernColors.getPrimary();
    }

    Color hoverBackground(float hoverAlpha) {
        Color hoverColor = UIManager.getColor("TabbedPane.hoverColor");
        if (hoverColor != null) {
            return new Color(
                    hoverColor.getRed(),
                    hoverColor.getGreen(),
                    hoverColor.getBlue(),
                    (int) (hoverColor.getAlpha() * hoverAlpha)
            );
        }

        return ModernColors.withAlpha(ModernColors.getHoverBackgroundColor(), Math.round(255 * hoverAlpha));
    }

    Color selectedBackground() {
        Color selectedBackground = UIManager.getColor("TabbedPane.selectedBackground");
        return selectedBackground != null ? selectedBackground : ModernColors.getHoverBackgroundColor();
    }
}

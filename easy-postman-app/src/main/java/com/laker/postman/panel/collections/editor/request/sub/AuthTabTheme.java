package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class AuthTabTheme {
    String titleColorHex() {
        return toHex(ModernColors.getAccent());
    }

    String descriptionColorHex() {
        return toHex(ModernColors.getTextHint());
    }

    String textColorHex() {
        return toHex(ModernColors.getTextSecondary());
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}

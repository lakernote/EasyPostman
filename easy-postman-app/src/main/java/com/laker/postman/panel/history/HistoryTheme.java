package com.laker.postman.panel.history;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class HistoryTheme {
    Color selectionBackground() {
        return ModernColors.getSelectionBackgroundColor();
    }

    Color hoverBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color selectedTitleForeground() {
        return ModernColors.getTextPrimary();
    }

    Color groupHeaderForeground() {
        return ModernColors.getTextSecondary();
    }

    String searchHighlightBackgroundHex() {
        return toHex(ModernColors.getSearchHighlightBackgroundColor());
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}

package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class RequestBodyTheme {
    Color definedVariableHighlight() {
        return ModernColors.getDefinedVariableBadgeBackground();
    }

    Color undefinedVariableHighlight() {
        return ModernColors.getUndefinedVariableBadgeBackground();
    }

    Color definedVariableBorder() {
        return ModernColors.getDefinedVariableBadgeBorder();
    }

    Color undefinedVariableBorder() {
        return ModernColors.getUndefinedVariableBadgeBorder();
    }

    Color popupBackground() {
        return ModernColors.getInputBackgroundColor();
    }

    Color popupSelectionBackground() {
        return ModernColors.getConsoleSelectionBg();
    }

    Color popupSelectionForeground() {
        return ModernColors.getTextPrimary();
    }

    Color popupValueForeground() {
        return ModernColors.getTextHint();
    }

    Color popupBorder() {
        return ModernColors.getBorderMediumColor();
    }

    Color tooltipDivider() {
        return ModernColors.getBorderMediumColor();
    }

    Color tooltipText() {
        return ModernColors.getTextPrimary();
    }

    Color tooltipMutedText() {
        return ModernColors.getTextHint();
    }

    Color tooltipCodeBackground() {
        return ModernColors.getHoverBackgroundColor();
    }
}

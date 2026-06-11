package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class SegmentedButtonTheme {
    Color segmentBackground() {
        return ModernColors.getInputBackgroundColor();
    }

    Color segmentBorder() {
        return ModernColors.getBorderLightColor();
    }

    Color segmentHoverBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color segmentPressedBackground() {
        return ModernColors.getButtonPressedColor();
    }

    Color selectedSegmentBackground() {
        return ModernColors.getPrimary();
    }

    Color selectedSegmentHoverBackground() {
        return ModernColors.getPrimaryLight();
    }

    Color selectedSegmentPressedBackground() {
        return ModernColors.getPrimaryDarker();
    }

    Color segmentText(boolean selected, boolean disabled) {
        if (disabled) {
            return ModernColors.getTextDisabled();
        }
        return selected ? ModernColors.getTextInverse() : ModernColors.getTextPrimary();
    }
}

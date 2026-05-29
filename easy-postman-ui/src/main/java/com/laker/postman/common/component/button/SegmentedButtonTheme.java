package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class SegmentedButtonTheme {
    Color segmentBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    Color segmentBorder() {
        return ModernColors.getBorderMediumColor();
    }
}

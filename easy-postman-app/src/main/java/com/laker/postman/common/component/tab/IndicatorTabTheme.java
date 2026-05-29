package com.laker.postman.common.component.tab;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class IndicatorTabTheme {
    Color indicator() {
        return ModernColors.getSuccess();
    }

    Color indicatorBorder() {
        return ModernColors.withAlpha(ModernColors.getSuccessDark(), 100);
    }
}

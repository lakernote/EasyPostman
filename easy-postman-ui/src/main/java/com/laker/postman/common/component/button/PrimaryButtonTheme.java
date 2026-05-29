package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class PrimaryButtonTheme {
    Color disabledBackground() {
        return ModernColors.getButtonDisabledBackgroundColor();
    }
}

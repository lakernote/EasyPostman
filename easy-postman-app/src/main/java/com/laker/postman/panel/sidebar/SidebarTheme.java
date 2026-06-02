package com.laker.postman.panel.sidebar;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.awt.GradientPaint;

@UtilityClass
class SidebarTheme {
    Color selectedTabBackground() {
        return ModernColors.primaryWithAlpha(25);
    }

    Color selectedTabTitleForeground() {
        return ModernColors.getPrimary();
    }

    GradientPaint selectedTabIndicatorPaint(int indicatorHeight) {
        return new GradientPaint(
                0, 0, ModernColors.getPrimary(),
                0, indicatorHeight, ModernColors.getPrimaryLight()
        );
    }
}

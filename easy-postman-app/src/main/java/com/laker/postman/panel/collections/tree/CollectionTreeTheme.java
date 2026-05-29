package com.laker.postman.panel.collections.tree;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class CollectionTreeTheme {
    private static final int HOVER_OVERLAY_ALPHA = 24;

    Color hoverOverlayColor() {
        return ModernColors.withAlpha(ModernColors.getTextPrimary(), HOVER_OVERLAY_ALPHA);
    }
}

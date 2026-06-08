package com.laker.postman.panel.sidebar;

import lombok.experimental.UtilityClass;

/**
 * 侧边栏 Tab 尺寸策略，避免绘制组件里散落 magic number。
 */
@UtilityClass
class SidebarTabMetrics {
    static final int TAB_AREA_INSET_TOP = 4;
    static final int TAB_AREA_INSET_LEFT = 4;
    static final int TAB_AREA_INSET_BOTTOM = 4;
    static final int TAB_AREA_INSET_RIGHT = 4;

    static final int EXPANDED_SELECTED_BACKGROUND_INSET_HORIZONTAL = 8;
    static final int EXPANDED_SELECTED_BACKGROUND_INSET_VERTICAL = 5;
    static final int COLLAPSED_SELECTED_BACKGROUND_WIDTH = 34;
    static final int COLLAPSED_SELECTED_BACKGROUND_HEIGHT = 32;
    static final int SELECTED_BACKGROUND_ARC = 10;

    static final int EXPANDED_TAB_PADDING_VERTICAL = 6;
    static final int EXPANDED_TAB_PADDING_HORIZONTAL = 12;
    static final int EXPANDED_TAB_SPACING_TOP = 3;
    static final int EXPANDED_TAB_SPACING_MIDDLE = 3;
    static final int EXPANDED_TAB_SPACING_BOTTOM = 4;

    static final int COLLAPSED_TAB_PADDING_VERTICAL = 8;
    static final int COLLAPSED_TAB_PADDING_HORIZONTAL = 11;

    static int expandedWidth(int maxTextWidth) {
        int width = maxTextWidth + (EXPANDED_TAB_PADDING_HORIZONTAL * 2);
        return Math.min(Math.max(width, 60), 140);
    }

    static int collapsedWidth(int maxIconWidth) {
        int width = maxIconWidth + (COLLAPSED_TAB_PADDING_HORIZONTAL * 2);
        return Math.max(width, 44);
    }

    static int expandedHeight(int maxIconHeight, int fontHeight) {
        int height = EXPANDED_TAB_PADDING_VERTICAL
                + EXPANDED_TAB_SPACING_TOP
                + maxIconHeight
                + EXPANDED_TAB_SPACING_MIDDLE
                + fontHeight
                + EXPANDED_TAB_SPACING_BOTTOM
                + EXPANDED_TAB_PADDING_VERTICAL;
        return Math.max(height, 60);
    }

    static int collapsedHeight(int maxIconHeight) {
        int height = maxIconHeight + (COLLAPSED_TAB_PADDING_VERTICAL * 2);
        return Math.max(height, 40);
    }
}

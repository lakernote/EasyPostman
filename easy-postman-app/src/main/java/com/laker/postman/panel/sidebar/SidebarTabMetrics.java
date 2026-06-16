package com.laker.postman.panel.sidebar;

import lombok.experimental.UtilityClass;

/**
 * 侧边栏 Tab 尺寸策略，避免绘制组件里散落 magic number。
 */
@UtilityClass
class SidebarTabMetrics {
    static final int TAB_AREA_INSET_TOP = 4;
    static final int TAB_AREA_INSET_LEFT = 2;
    static final int TAB_AREA_INSET_BOTTOM = 4;
    static final int TAB_AREA_INSET_RIGHT = 2;

    // 展开态右侧紧邻内容卡片，卡片自身还有 gutter；右侧少留一点，整体看起来才左右均衡。
    static final int EXPANDED_SELECTED_BACKGROUND_INSET_LEFT = 10;
    static final int EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT = 2;
    static final int EXPANDED_SELECTED_BACKGROUND_INSET_VERTICAL = 5;
    static final int COLLAPSED_SELECTED_BACKGROUND_WIDTH = 32;
    static final int COLLAPSED_SELECTED_BACKGROUND_HEIGHT = 32;
    // 折叠态 tab rect 从 rail 最左侧开始，tab area 的左右 inset 计入整条 rail。
    // 这里向右补偿 2px，让 32px 选中背景在 40px rail 内形成左右各 4px 的视觉边距。
    static final int COLLAPSED_VISUAL_CENTER_OFFSET_X = 2;
    static final int SELECTED_BACKGROUND_ARC = 10;

    static final int EXPANDED_TAB_PADDING_VERTICAL = 6;
    static final int EXPANDED_TAB_PADDING_HORIZONTAL = 12;
    static final int EXPANDED_TAB_SPACING_TOP = 3;
    static final int EXPANDED_TAB_SPACING_MIDDLE = 3;
    static final int EXPANDED_TAB_SPACING_BOTTOM = 4;

    static final int COLLAPSED_TAB_PADDING_VERTICAL = 8;
    // 选中背景做了右移补偿，图标左右 padding 也随之变成 8/4，避免图标偏左。
    static final int COLLAPSED_TAB_PADDING_HORIZONTAL = 6;

    static int expandedWidth(int maxTextWidth) {
        int width = maxTextWidth + (EXPANDED_TAB_PADDING_HORIZONTAL * 2);
        return Math.min(Math.max(width, 60), 140);
    }

    static int collapsedWidth(int maxIconWidth) {
        int width = maxIconWidth + (COLLAPSED_TAB_PADDING_HORIZONTAL * 2);
        return Math.max(width, 36);
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

    static int expandedSelectedBackgroundX(int tabX) {
        return tabX + EXPANDED_SELECTED_BACKGROUND_INSET_LEFT;
    }

    static int expandedSelectedBackgroundWidth(int tabWidth) {
        return Math.max(0, tabWidth
                - EXPANDED_SELECTED_BACKGROUND_INSET_LEFT
                - EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT);
    }

    static int expandedContentPaddingLeft() {
        return EXPANDED_TAB_PADDING_HORIZONTAL
                + (EXPANDED_SELECTED_BACKGROUND_INSET_LEFT - EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT) / 2;
    }

    static int expandedContentPaddingRight() {
        return Math.max(0, EXPANDED_TAB_PADDING_HORIZONTAL
                - (EXPANDED_SELECTED_BACKGROUND_INSET_LEFT - EXPANDED_SELECTED_BACKGROUND_INSET_RIGHT) / 2);
    }

    static int collapsedSelectedBackgroundX(int tabX, int tabWidth, int backgroundWidth) {
        int availableSpace = Math.max(0, tabWidth - backgroundWidth);
        int centeredX = availableSpace / 2 + COLLAPSED_VISUAL_CENTER_OFFSET_X;
        return tabX + Math.min(centeredX, availableSpace);
    }

    static int collapsedIconPaddingLeft() {
        return COLLAPSED_TAB_PADDING_HORIZONTAL + COLLAPSED_VISUAL_CENTER_OFFSET_X;
    }

    static int collapsedIconPaddingRight() {
        return Math.max(0, COLLAPSED_TAB_PADDING_HORIZONTAL - COLLAPSED_VISUAL_CENTER_OFFSET_X);
    }
}

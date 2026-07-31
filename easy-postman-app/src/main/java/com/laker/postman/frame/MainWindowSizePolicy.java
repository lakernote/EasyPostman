package com.laker.postman.frame;

import lombok.experimental.UtilityClass;

import java.awt.Dimension;

/**
 * 主窗口尺寸策略，区分首启默认尺寸和用户可缩放的最小尺寸。
 */
@UtilityClass
class MainWindowSizePolicy {
    private static final int SCREEN_WIDTH_4K = 3840;
    private static final int SCREEN_WIDTH_2K = 2560;
    private static final int SCREEN_WIDTH_FHD = 1920;
    private static final int SCREEN_WIDTH_HD = 1280;
    private static final int SCREEN_WIDTH_MAXIMIZED_THRESHOLD = 1366;

    private static final Dimension DEFAULT_SIZE_4K = new Dimension(1920, 1200);
    private static final Dimension DEFAULT_SIZE_2K = new Dimension(1600, 1000);
    private static final Dimension DEFAULT_SIZE_FHD = new Dimension(1400, 900);
    private static final Dimension DEFAULT_SIZE_HD = new Dimension(1280, 800);
    private static final Dimension DEFAULT_SIZE_WXGA = new Dimension(1100, 700);

    /**
     * 应用布局在常规显示器上的设计下限。实际最小尺寸还会受所有显示器可用工作区约束，
     * 避免主屏生成的最小尺寸在较小副屏上导致窗口无法还原或缩放。
     */
    private static final Dimension DESIGN_MINIMUM_SIZE = new Dimension(1100, 700);

    static Dimension preferredSizeForScreenWidth(double screenWidth) {
        if (screenWidth >= SCREEN_WIDTH_4K) {
            return copyOf(DEFAULT_SIZE_4K);
        }
        if (screenWidth >= SCREEN_WIDTH_2K) {
            return copyOf(DEFAULT_SIZE_2K);
        }
        if (screenWidth >= SCREEN_WIDTH_FHD) {
            return copyOf(DEFAULT_SIZE_FHD);
        }
        if (screenWidth >= SCREEN_WIDTH_HD) {
            return copyOf(DEFAULT_SIZE_HD);
        }
        return copyOf(DEFAULT_SIZE_WXGA);
    }

    static Dimension minimumSizeForDisplayAreas(Dimension... usableDisplayAreas) {
        int minimumWidth = DESIGN_MINIMUM_SIZE.width;
        int minimumHeight = DESIGN_MINIMUM_SIZE.height;

        if (usableDisplayAreas != null) {
            for (Dimension displayArea : usableDisplayAreas) {
                if (displayArea == null || displayArea.width <= 0 || displayArea.height <= 0) {
                    continue;
                }
                minimumWidth = Math.min(minimumWidth, displayArea.width);
                minimumHeight = Math.min(minimumHeight, displayArea.height);
            }
        }

        return new Dimension(minimumWidth, minimumHeight);
    }

    static Dimension constrainToUsableArea(Dimension requestedSize,
                                           Dimension minimumSize,
                                           Dimension usableDisplayArea) {
        int usableWidth = positiveOrMaxValue(usableDisplayArea == null ? 0 : usableDisplayArea.width);
        int usableHeight = positiveOrMaxValue(usableDisplayArea == null ? 0 : usableDisplayArea.height);
        int minimumWidth = Math.min(positiveOrOne(minimumSize == null ? 0 : minimumSize.width), usableWidth);
        int minimumHeight = Math.min(positiveOrOne(minimumSize == null ? 0 : minimumSize.height), usableHeight);
        int requestedWidth = positiveOrOne(requestedSize == null ? 0 : requestedSize.width);
        int requestedHeight = positiveOrOne(requestedSize == null ? 0 : requestedSize.height);

        int width = Math.min(Math.max(requestedWidth, minimumWidth), usableWidth);
        int height = Math.min(Math.max(requestedHeight, minimumHeight), usableHeight);
        return new Dimension(width, height);
    }

    static boolean shouldStartMaximized(double screenWidth) {
        return screenWidth <= SCREEN_WIDTH_MAXIMIZED_THRESHOLD;
    }

    private static int positiveOrOne(int value) {
        return value > 0 ? value : 1;
    }

    private static int positiveOrMaxValue(int value) {
        return value > 0 ? value : Integer.MAX_VALUE;
    }

    private static Dimension copyOf(Dimension dimension) {
        return new Dimension(dimension);
    }
}

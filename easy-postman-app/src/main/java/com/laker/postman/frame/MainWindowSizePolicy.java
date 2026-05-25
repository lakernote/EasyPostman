package com.laker.postman.frame;

import lombok.experimental.UtilityClass;

import java.awt.Dimension;

/**
 * 主窗口尺寸策略，集中管理不同屏幕宽度下的默认窗口尺寸。
 */
@UtilityClass
class MainWindowSizePolicy {
    private static final int SCREEN_WIDTH_4K = 3840;
    private static final int SCREEN_WIDTH_2K = 2560;
    private static final int SCREEN_WIDTH_FHD = 1920;
    private static final int SCREEN_WIDTH_HD = 1280;
    private static final int SCREEN_WIDTH_MAXIMIZED_THRESHOLD = 1366;

    private static final Dimension MIN_SIZE_4K = new Dimension(1920, 1200);
    private static final Dimension MIN_SIZE_2K = new Dimension(1600, 1000);
    private static final Dimension MIN_SIZE_FHD = new Dimension(1400, 900);
    private static final Dimension MIN_SIZE_HD = new Dimension(1280, 800);
    private static final Dimension MIN_SIZE_WXGA = new Dimension(1100, 700);

    static Dimension minimumSizeForScreenWidth(double screenWidth) {
        if (screenWidth >= SCREEN_WIDTH_4K) {
            return copyOf(MIN_SIZE_4K);
        }
        if (screenWidth >= SCREEN_WIDTH_2K) {
            return copyOf(MIN_SIZE_2K);
        }
        if (screenWidth >= SCREEN_WIDTH_FHD) {
            return copyOf(MIN_SIZE_FHD);
        }
        if (screenWidth >= SCREEN_WIDTH_HD) {
            return copyOf(MIN_SIZE_HD);
        }
        return copyOf(MIN_SIZE_WXGA);
    }

    static boolean shouldStartMaximized(double screenWidth) {
        return screenWidth <= SCREEN_WIDTH_MAXIMIZED_THRESHOLD;
    }

    private static Dimension copyOf(Dimension dimension) {
        return new Dimension(dimension);
    }
}

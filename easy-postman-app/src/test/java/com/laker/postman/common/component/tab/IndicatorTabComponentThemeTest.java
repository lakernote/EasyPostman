package com.laker.postman.common.component.tab;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class IndicatorTabComponentThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.SUCCESS, ThemeColors.SUCCESS_DARK);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void indicatorColorsShouldUseContrastStableSuccessThemeToken() {
        Color indicator = new Color(11, 92, 12);
        UIManager.put(ThemeColors.SUCCESS_DARK, indicator);

        assertEquals(IndicatorTabTheme.indicator(), indicator);
        assertEquals(IndicatorTabTheme.indicatorBorder(), ModernColors.withAlpha(indicator, 100));
    }

    @Test
    public void indicatorShouldKeepPaintedContentAwayFromRightClip() {
        IndicatorTabComponent tab = new IndicatorTabComponent("Request Headers");
        tab.setShowIndicator(true);
        tab.setSize(tab.getPreferredSize());

        BufferedImage image = new BufferedImage(tab.getWidth(), tab.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            tab.paint(graphics);
        } finally {
            graphics.dispose();
        }

        int rightMostPaintedPixel = rightMostPaintedPixel(image);
        assertTrue(
                rightMostPaintedPixel < image.getWidth() - 1,
                "Painted content touches the component clip at x=" + rightMostPaintedPixel
                        + " for width=" + image.getWidth()
        );
    }

    private static int rightMostPaintedPixel(BufferedImage image) {
        for (int x = image.getWidth() - 1; x >= 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) != 0) {
                    return x;
                }
            }
        }
        return -1;
    }
}

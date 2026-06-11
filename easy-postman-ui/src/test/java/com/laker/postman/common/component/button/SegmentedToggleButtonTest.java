package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SegmentedToggleButtonTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.HOVER_BACKGROUND,
                ThemeColors.PRIMARY,
                ThemeColors.TEXT_DISABLED
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void rolloverBackgroundShouldUseHoverToken() {
        Color hover = new Color(33, 44, 55);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, hover);
        SegmentedToggleButton button = new SegmentedToggleButton("HTTP", false);
        button.getModel().setRollover(true);

        Color center = centerPixel(render(button));

        assertTrue(isNear(center, hover), "Segment hover state should use the shared hover background token");
    }

    @Test
    public void selectedBackgroundShouldUsePrimaryToken() {
        Color primary = new Color(55, 113, 225);
        UIManager.put(ThemeColors.PRIMARY, primary);
        SegmentedToggleButton button = new SegmentedToggleButton("HTTP", true);

        Color center = centerPixel(render(button));

        assertTrue(isNear(center, primary), "Selected segment should use the primary brand token");
    }

    @Test
    public void selectedBackgroundShouldLeaveBreathingRoomAtTopEdge() {
        SegmentedToggleButton button = new SegmentedToggleButton("HTTP", true);

        Color topEdge = colorAt(render(button), 46, 2);

        assertTrue(topEdge.getAlpha() < 64, "Selected segment should not fill tightly to the container edge");
    }

    @Test
    public void disabledSelectedButtonShouldUseDisabledTextColor() {
        Color disabled = new Color(90, 91, 92);
        UIManager.put(ThemeColors.TEXT_DISABLED, disabled);
        SegmentedToggleButton button = new SegmentedToggleButton("HTTP", true);
        button.setEnabled(false);

        render(button);

        assertEquals(button.getForeground(), disabled);
    }

    private static BufferedImage render(AbstractButton button) {
        button.setSize(92, 30);
        button.doLayout();
        BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Color centerPixel(BufferedImage image) {
        return new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2), true);
    }

    private static Color colorAt(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y), true);
    }

    private static boolean isNear(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= 2
                && Math.abs(actual.getGreen() - expected.getGreen()) <= 2
                && Math.abs(actual.getBlue() - expected.getBlue()) <= 2
                && actual.getAlpha() > 250;
    }
}

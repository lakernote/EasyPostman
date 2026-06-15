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
import static org.testng.Assert.assertTrue;

public class ModernButtonFactoryTest {

    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.BUTTON_DISABLED_BACKGROUND, ThemeColors.TEXT_DISABLED);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void disabledPrimaryBackgroundShouldUseSemanticButtonDisabledToken() {
        Color disabledBackground = new Color(31, 32, 33);
        UIManager.put(ThemeColors.BUTTON_DISABLED_BACKGROUND, disabledBackground);
        UIManager.put(ThemeColors.TEXT_DISABLED, new Color(96, 97, 98));

        AbstractButton button = ModernButtonFactory.createButton("", true);
        button.setEnabled(false);

        Color center = centerPixel(render(button));

        assertTrue(isNear(center, disabledBackground),
                "Disabled primary buttons should not paint the disabled text color as their background");
    }

    private static BufferedImage render(AbstractButton button) {
        button.setSize(100, 34);
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

    private static boolean isNear(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= 2
                && Math.abs(actual.getGreen() - expected.getGreen()) <= 2
                && Math.abs(actual.getBlue() - expected.getBlue()) <= 2
                && actual.getAlpha() > 250;
    }
}

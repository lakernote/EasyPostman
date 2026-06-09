package com.laker.postman.common.component.button;

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
import static org.testng.Assert.assertTrue;

public class PrimaryButtonTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.PRIMARY, ThemeColors.PRIMARY_LIGHT);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void defaultBackgroundShouldUsePrimaryToken() {
        Color primary = new Color(55, 113, 225);
        UIManager.put(ThemeColors.PRIMARY, primary);

        Color center = centerPixel(render(new PrimaryButton("")));

        assertTrue(isNear(center, primary), "PrimaryButton normal state should use the primary brand blue");
    }

    @Test
    public void rolloverBackgroundShouldUsePrimaryLightToken() {
        Color primaryLight = new Color(94, 143, 240);
        UIManager.put(ThemeColors.PRIMARY_LIGHT, primaryLight);
        PrimaryButton button = new PrimaryButton("");
        button.getModel().setRollover(true);

        Color center = centerPixel(render(button));

        assertTrue(isNear(center, primaryLight), "PrimaryButton hover state should use the lighter primary blue");
    }

    private static BufferedImage render(AbstractButton button) {
        button.setSize(90, 34);
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

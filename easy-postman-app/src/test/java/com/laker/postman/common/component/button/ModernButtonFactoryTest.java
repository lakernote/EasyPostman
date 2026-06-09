package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatLightLaf;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.Icon;
import javax.swing.AbstractButton;
import javax.swing.JButton;
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

    @BeforeClass
    public void installLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(new FlatLightLaf());
    }

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.PRIMARY, ThemeColors.PRIMARY_LIGHT);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test(description = "Primary ModernButtonFactory buttons should render icons with on-primary foreground")
    public void shouldRenderPrimaryButtonIconWithOnPrimaryColor() {
        JButton button = ModernButtonFactory.createButton("Import", true, "icons/import.svg", 16);

        assertTrue(hasVisiblePixelsNear(button.getIcon(), button, ModernColors.getTextInverse()),
                "Primary button icon should use the on-primary foreground color");
    }

    @Test
    public void primaryButtonBackgroundShouldUsePrimaryToken() {
        Color primary = new Color(55, 113, 225);
        UIManager.put(ThemeColors.PRIMARY, primary);

        Color center = centerPixel(render(ModernButtonFactory.createButton("", true)));

        assertTrue(isNear(center, primary), "Modern primary buttons should use the primary brand blue");
    }

    @Test
    public void primaryButtonRolloverBackgroundShouldUsePrimaryLightToken() {
        Color primaryLight = new Color(94, 143, 240);
        UIManager.put(ThemeColors.PRIMARY_LIGHT, primaryLight);
        JButton button = ModernButtonFactory.createButton("", true);
        button.getModel().setRollover(true);

        Color center = centerPixel(render(button));

        assertTrue(isNear(center, primaryLight), "Modern primary button hover should use the lighter primary blue");
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

    private static boolean hasVisiblePixelsNear(Icon icon, JButton button, Color expectedColor) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(button, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (pixel.getAlpha() > 32 && isNear(pixel, expectedColor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNear(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= 3
                && Math.abs(actual.getGreen() - expected.getGreen()) <= 3
                && Math.abs(actual.getBlue() - expected.getBlue()) <= 3;
    }
}

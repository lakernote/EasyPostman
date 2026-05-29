package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatLightLaf;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.testng.Assert.assertTrue;

public class ModernButtonFactoryTest {

    @BeforeClass
    public void installLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(new FlatLightLaf());
    }

    @Test(description = "Primary ModernButtonFactory buttons should render icons with on-primary foreground")
    public void shouldRenderPrimaryButtonIconWithOnPrimaryColor() {
        JButton button = ModernButtonFactory.createButton("Import", true, "icons/import.svg", 16);

        assertTrue(hasVisiblePixelsNear(button.getIcon(), button, Color.WHITE),
                "Primary button icon should use the on-primary foreground color");
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

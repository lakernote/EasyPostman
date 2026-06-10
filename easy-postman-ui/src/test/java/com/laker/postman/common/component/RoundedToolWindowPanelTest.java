package com.laker.postman.common.component;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.test.ThemeTokenTestSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RoundedToolWindowPanelTest {
    private Map<String, Object> previousTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTokens = ThemeTokenTestSupport.remember(
                ThemeColors.BACKGROUND,
                ThemeColors.SURFACE
        );
    }

    @AfterMethod
    public void restoreThemeTokens() {
        ThemeTokenTestSupport.restore(previousTokens);
    }

    @Test
    public void shouldUseIdeaLikeDefaultCornerRadius() {
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(new JLabel("content"));

        assertEquals(panel.getCornerArc(), 20);
    }

    @Test
    public void shouldWrapContentWithOnePixelCardInset() {
        JLabel content = new JLabel("content");

        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(content);

        assertSame(panel.getComponent(0), content);
        assertTrue(panel.isOpaque());
        assertEquals(panel.getLayout().getClass(), BorderLayout.class);
        assertEquals(panel.getInsets().top, 1);
        assertEquals(panel.getInsets().left, 1);
        assertEquals(panel.getInsets().bottom, 1);
        assertEquals(panel.getInsets().right, 1);
    }

    @Test
    public void shouldClipOpaqueContentToRoundedShape() {
        UIManager.put(ThemeColors.BACKGROUND, new Color(238, 242, 247));
        UIManager.put(ThemeColors.SURFACE, new Color(245, 247, 250));
        JPanel content = new JPanel();
        content.setOpaque(true);
        content.setBackground(Color.RED);
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(content);
        panel.setSize(48, 48);
        panel.doLayout();

        BufferedImage image = paint(panel, 48, 48);

        Color clippedCorner = new Color(image.getRGB(2, 2), true);
        Color innerContent = new Color(image.getRGB(16, 16), true);
        assertTrue(clippedCorner.getGreen() > 0, "opaque child content must be clipped away from rounded corners");
        assertEquals(innerContent, Color.RED);
    }

    @Test
    public void shouldOwnDescendantRepaintsSoRoundedClipIsPreserved() {
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(new JLabel("content"));

        assertTrue(panel.isPaintingOrigin());
    }

    @Test
    public void shouldClearOuterBoundsWithMainBackgroundBeforePaintingRoundedCard() {
        Color background = new Color(238, 242, 247);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.SURFACE, new Color(245, 247, 250));
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(new JLabel("content"));
        panel.setSize(48, 48);
        panel.doLayout();

        BufferedImage image = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 48, 48);
        panel.paint(g2);
        g2.dispose();

        assertEquals(new Color(image.getRGB(0, 0), true), background);
    }

    @Test
    public void shouldPaintBorderlessRoundedCardSurface() {
        Color surface = new Color(245, 247, 250);
        UIManager.put(ThemeColors.SURFACE, surface);
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(new JLabel("content"));
        panel.setSize(48, 48);
        panel.doLayout();

        BufferedImage image = paint(panel, 48, 48);

        assertEquals(new Color(image.getRGB(24, 0), true), surface);
        assertEquals(new Color(image.getRGB(24, 2), true), surface);
    }

    @Test
    public void shouldKeepOutermostCardEdgeAfterOpaqueChildDirectRepaint() {
        Color background = new Color(238, 242, 247);
        Color surface = new Color(245, 247, 250);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.SURFACE, surface);
        JPanel content = new JPanel();
        content.setOpaque(true);
        content.setBackground(Color.RED);
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(content);
        panel.setSize(48, 48);
        panel.doLayout();

        BufferedImage image = paint(panel, 48, 48);
        Graphics2D childGraphics = image.createGraphics();
        childGraphics.translate(content.getX(), content.getY());
        content.paint(childGraphics);
        childGraphics.dispose();

        assertEquals(new Color(image.getRGB(47, 24), true), background);
    }

    @Test
    public void shouldNotPaintVisibleChromeDuringBorderPhase() {
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(new JLabel("content"));
        panel.setSize(48, 48);
        panel.doLayout();

        BufferedImage image = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paintBorder(g2);
        g2.dispose();

        assertEquals(new Color(image.getRGB(24, 0), true).getAlpha(), 0);
    }

    @Test
    public void shouldUseSurfaceThemeToken() {
        Color surface = new Color(245, 247, 250);
        UIManager.put(ThemeColors.SURFACE, surface);
        RoundedToolWindowPanel panel = new RoundedToolWindowPanel(new JLabel("content"));

        assertEquals(panel.cardBackgroundColor(), surface);
    }

    private static BufferedImage paint(RoundedToolWindowPanel panel, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();
        return image;
    }
}

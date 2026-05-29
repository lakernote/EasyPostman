package com.laker.postman.panel.collections.tree;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CollectionTreePanelThemeTest {
    private Object previousTextPrimary;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTextPrimary = UIManager.get(ThemeColors.TEXT_PRIMARY);
    }

    @AfterMethod
    public void restoreThemeTokens() {
        UIManager.put(ThemeColors.TEXT_PRIMARY, previousTextPrimary);
    }

    @Test
    public void shouldUseTransparentThemeOverlayForTreeHoverBackground() {
        Color textPrimary = new Color(31, 47, 63);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);

        assertEquals(CollectionTreeTheme.hoverOverlayColor(), new Color(31, 47, 63, 24));
    }

    @Test
    public void hoverOverlayShouldRemainTranslucentBecauseItIsPaintedAfterTreeText() {
        UIManager.put(ThemeColors.TEXT_PRIMARY, new Color(20, 30, 40));

        Color overlay = CollectionTreeTheme.hoverOverlayColor();

        assertTrue(overlay.getAlpha() > 0, "hover overlay should still be visible");
        assertTrue(overlay.getAlpha() <= 64, "hover overlay must not cover already-painted tree text");
    }

    @Test
    public void hoverOverlayPainterShouldFillHoveredUnselectedRowWithTranslucentOverlay() {
        UIManager.put(ThemeColors.TEXT_PRIMARY, new Color(20, 30, 40));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(CollectionTreePanel.ROOT);
        JTree tree = new JTree(root);
        RequestTreeCellRenderer renderer = new RequestTreeCellRenderer();
        renderer.setHoveredRow(0);
        tree.setCellRenderer(renderer);
        tree.setSize(120, 32);
        tree.setRowHeight(24);

        BufferedImage image = new BufferedImage(120, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        CollectionTreeHoverOverlay.paint(g2, tree);
        g2.dispose();

        Color painted = new Color(image.getRGB(2, 12), true);
        assertEquals(painted.getAlpha(), 24);
        assertClose(painted.getRed(), 20);
        assertClose(painted.getGreen(), 30);
        assertClose(painted.getBlue(), 40);
    }

    private static void assertClose(int actual, int expected) {
        assertTrue(Math.abs(actual - expected) <= 3, "expected " + actual + " to be close to " + expected);
    }
}

package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * Shared rounded tool-window chrome for IDEA-like side/content panes.
 */
public class RoundedToolWindowPanel extends JPanel {
    private static final int DEFAULT_CORNER_ARC = 20;
    private final int cornerArc;

    public RoundedToolWindowPanel(Component content) {
        this(content, DEFAULT_CORNER_ARC);
    }

    public RoundedToolWindowPanel(Component content, int cornerArc) {
        super(new BorderLayout());
        this.cornerArc = cornerArc;
        putClientProperty(ToolWindowChrome.CHROME_ROUNDED_PROPERTY, Boolean.TRUE);
        setOpaque(true);
        refreshBackground();
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        add(content, BorderLayout.CENTER);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        refreshBackground();
    }

    public int getCornerArc() {
        return cornerArc;
    }

    @Override
    protected boolean isPaintingOrigin() {
        // Descendant repaint requests must go through this panel so the rounded clip is preserved.
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            enableQuality(g2);
            g2.setColor(cardBackgroundColor());
            g2.fill(roundedShape());
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            enableQuality(g2);
            g2.clip(innerRoundedShape());
            super.paintChildren(g2);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
        // IDEA-like tool windows are separated by background gaps, not by visible strokes.
    }

    Color cardBackgroundColor() {
        return ModernColors.getCardBackgroundColor();
    }

    private void refreshBackground() {
        setBackground(ModernColors.getBackgroundColor());
    }

    private Shape roundedShape() {
        int width = Math.max(0, getWidth() - 1);
        int height = Math.max(0, getHeight() - 1);
        return new RoundRectangle2D.Float(0, 0, width, height, cornerArc, cornerArc);
    }

    private Shape innerRoundedShape() {
        int width = Math.max(0, getWidth() - 3);
        int height = Math.max(0, getHeight() - 3);
        int innerArc = Math.max(0, cornerArc - 2);
        return new RoundRectangle2D.Float(1, 1, width, height, innerArc, innerArc);
    }

    private static void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}

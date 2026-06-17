package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Compact semantic label used for status/detail chips.
 */
public class ChipLabel extends JLabel {
    private static final int ARC = 8;
    private static final int FILL_ALPHA = 30;
    private static final int BORDER_ALPHA = 140;

    private Color accentColor;

    public ChipLabel() {
        this("", null);
    }

    public ChipLabel(String text) {
        this(text, null);
    }

    public ChipLabel(String text, Color accentColor) {
        super(text);
        this.accentColor = accentColor;
        setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        setBorder(new EmptyBorder(2, 6, 2, 6));
        setOpaque(false);
        updateForeground();
    }

    public void setChip(String text, Color accentColor) {
        setText(text);
        setAccentColor(accentColor);
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(Color accentColor) {
        this.accentColor = accentColor;
        updateForeground();
        repaint();
    }

    public static Color foregroundFor(Color accentColor) {
        if (accentColor == null) {
            return ModernColors.getTextPrimary();
        }
        return ModernColors.isDarkTheme()
                ? ModernColors.blendColors(accentColor, ModernColors.getTextInverse(), 0.45f)
                : accentColor.darker();
    }

    public static Color fillFor(Color accentColor, int alpha) {
        Color base = accentColor == null ? ModernColors.getDividerBorderColor() : accentColor;
        return ModernColors.withAlpha(base, alpha);
    }

    public static Color borderFor(Color accentColor, int alpha) {
        return fillFor(accentColor, alpha);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateForeground();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateForeground();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (accentColor != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D.Float chipShape = chipShape();
            g2.setColor(fillFor(accentColor, FILL_ALPHA));
            g2.fill(chipShape);
            g2.setColor(borderFor(accentColor, BORDER_ALPHA));
            g2.draw(chipShape);
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private RoundRectangle2D.Float chipShape() {
        return new RoundRectangle2D.Float(0.5f, 0.5f,
                Math.max(0f, getWidth() - 1f),
                Math.max(0f, getHeight() - 1f),
                ARC, ARC);
    }

    private void updateForeground() {
        if (!isEnabled()) {
            setForeground(ModernColors.getTextDisabled());
            return;
        }
        setForeground(foregroundFor(accentColor));
    }
}

package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.util.FontsUtil;
import lombok.Getter;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Compact count badge used by tab buttons.
 */
@Getter
final class TabCountBadgeIcon implements Icon {
    private static final int HEIGHT = 18;
    private static final int HORIZONTAL_PADDING = 6;
    private static final int MIN_WIDTH = 18;

    private final String text;
    private final Color foregroundColor;
    private final Color backgroundColor;
    private final Color borderColor;
    private final Font font;
    private final int width;

    TabCountBadgeIcon(JComponent owner,
                      String text,
                      Color foregroundColor,
                      Color backgroundColor,
                      Color borderColor) {
        this.text = text;
        this.foregroundColor = foregroundColor;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.font = resolveFont(owner);
        FontMetrics metrics = owner.getFontMetrics(font);
        this.width = Math.max(MIN_WIDTH, metrics.stringWidth(text) + HORIZONTAL_PADDING * 2);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int arc = HEIGHT;
        g2.setColor(backgroundColor);
        g2.fillRoundRect(x, y, width, HEIGHT, arc, arc);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, width - 1, HEIGHT - 1, arc, arc);
        }

        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = y + (HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent() - 1;
        g2.setColor(foregroundColor);
        g2.drawString(text, textX, textY);
        g2.dispose();
    }

    private static Font resolveFont(JComponent owner) {
        Font ownerFont = owner.getFont();
        if (ownerFont != null) {
            return ownerFont.deriveFont(Font.BOLD);
        }
        return FontsUtil.getDefaultFont(Font.BOLD);
    }
}

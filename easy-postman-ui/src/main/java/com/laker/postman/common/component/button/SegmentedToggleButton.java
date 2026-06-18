package com.laker.postman.common.component.button;

import com.laker.postman.util.FontsUtil;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class SegmentedToggleButton extends JToggleButton {
    private static final int DEFAULT_ARC = 8;
    private static final int DEFAULT_BACKGROUND_INSET = 3;
    private static final int DEFAULT_MIN_HEIGHT = 28;
    private static final int DEFAULT_HORIZONTAL_PADDING = 14;
    private final int arc;
    private final int backgroundInset;
    private final int minHeight;

    public SegmentedToggleButton(String text, boolean selected) {
        this(text, selected, DEFAULT_HORIZONTAL_PADDING);
    }

    public SegmentedToggleButton(String text, boolean selected, int horizontalPadding) {
        this(text, selected, horizontalPadding, DEFAULT_MIN_HEIGHT, DEFAULT_BACKGROUND_INSET, DEFAULT_ARC);
    }

    SegmentedToggleButton(String text,
                          boolean selected,
                          int horizontalPadding,
                          int minHeight,
                          int backgroundInset,
                          int arc) {
        super(text, selected);
        this.minHeight = minHeight;
        this.backgroundInset = backgroundInset;
        this.arc = arc;
        setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        setBorder(BorderFactory.createEmptyBorder(4, horizontalPadding, 4, horizontalPadding));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        setForeground(SegmentedButtonTheme.segmentText(isSelected(), !isEnabled()));

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color background = resolveSegmentBackground();
        if (background != null) {
            int width = getWidth() - backgroundInset * 2;
            int height = getHeight() - backgroundInset * 2;
            if (width > 0 && height > 0) {
                g2.setColor(background);
                g2.fillRoundRect(backgroundInset, backgroundInset, width, height, arc, arc);
            }
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(size.height, minHeight);
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.height = Math.max(size.height, minHeight);
        return size;
    }

    private Color resolveSegmentBackground() {
        if (!isEnabled()) {
            return null;
        }

        ButtonModel model = getModel();
        if (isSelected()) {
            if (model.isPressed()) {
                return SegmentedButtonTheme.selectedSegmentPressedBackground();
            }
            if (model.isRollover()) {
                return SegmentedButtonTheme.selectedSegmentHoverBackground();
            }
            return SegmentedButtonTheme.selectedSegmentBackground();
        }
        if (model.isPressed()) {
            return SegmentedButtonTheme.segmentPressedBackground();
        }
        if (model.isRollover()) {
            return SegmentedButtonTheme.segmentHoverBackground();
        }
        return null;
    }
}

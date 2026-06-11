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
    private static final int ARC = 8;
    private static final int BACKGROUND_INSET = 3;
    private static final int MIN_HEIGHT = 28;

    public SegmentedToggleButton(String text, boolean selected) {
        super(text, selected);
        setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
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
            int width = getWidth() - BACKGROUND_INSET * 2;
            int height = getHeight() - BACKGROUND_INSET * 2;
            if (width > 0 && height > 0) {
                g2.setColor(background);
                g2.fillRoundRect(BACKGROUND_INSET, BACKGROUND_INSET, width, height, ARC, ARC);
            }
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(size.height, MIN_HEIGHT);
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.height = Math.max(size.height, MIN_HEIGHT);
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

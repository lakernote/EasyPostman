package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class SegmentedToggleButton extends JToggleButton {

    public SegmentedToggleButton(String text, boolean selected) {
        super(text, selected);
        setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (isSelected()) {
            g2.setColor(getSelectedSegmentColor());
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 6, 6);
        } else if (getModel().isRollover()) {
            g2.setColor(getSegmentHoverColor());
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 6, 6);
        }
        g2.dispose();
        setForeground(isSelected() ? ModernColors.getTextInverse() : getTextColor());
        super.paintComponent(g);
    }

    private Color getSegmentHoverColor() {
        return ModernColors.getButtonPressedColor();
    }

    private Color getSelectedSegmentColor() {
        return ModernColors.getPrimary();
    }

    private Color getTextColor() {
        return ModernColors.getTextPrimary();
    }
}

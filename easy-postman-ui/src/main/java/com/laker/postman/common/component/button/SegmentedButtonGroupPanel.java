package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class SegmentedButtonGroupPanel extends JPanel {

    public SegmentedButtonGroupPanel(int alignment) {
        super(new FlowLayout(alignment, 2, 2));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getSegmentBackgroundColor());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.setColor(getSegmentBorderColor());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.dispose();
        super.paintComponent(g);
    }

    static boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    static Color getSegmentBackgroundColor() {
        return isDarkTheme() ? new Color(58, 58, 58) : new Color(238, 242, 247);
    }

    static Color getSegmentBorderColor() {
        return isDarkTheme() ? new Color(90, 90, 90) : new Color(194, 211, 236);
    }
}

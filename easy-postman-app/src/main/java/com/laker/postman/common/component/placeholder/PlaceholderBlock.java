package com.laker.postman.common.component.placeholder;

import javax.swing.*;
import java.awt.*;

final class PlaceholderBlock extends JComponent {

    private final Color fillColor;
    private final int arc;

    PlaceholderBlock(int width, int height, Color fillColor, int arc) {
        this.fillColor = fillColor;
        this.arc = arc;
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } finally {
            g2.dispose();
        }
    }
}

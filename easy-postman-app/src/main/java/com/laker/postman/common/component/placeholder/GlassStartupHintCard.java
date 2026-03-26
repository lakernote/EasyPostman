package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import java.awt.*;

public class GlassStartupHintCard extends JPanel {

    public GlassStartupHintCard() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 4));
        setPreferredSize(new Dimension(620, 210));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int innerX = 20;
            int innerY = 18;
            int innerWidth = width - 44;
            int innerHeight = height - 42;

            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(4, 8, width - 8, height - 8, 30, 30);

            GradientPaint background = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 82),
                    width, height, new Color(255, 255, 255, 42)
            );
            g2.setPaint(background);
            g2.fillRoundRect(0, 0, width - 4, height - 10, 28, 28);

            GradientPaint innerGlow = new GradientPaint(
                    0, 12, new Color(255, 255, 255, 58),
                    width, height - 12, new Color(255, 255, 255, 18)
            );
            g2.setPaint(innerGlow);
            g2.fillRoundRect(innerX, innerY, innerWidth, innerHeight, 26, 26);

            g2.setColor(ModernColors.primaryWithAlpha(22));
            g2.fillOval(width - 160, -20, 180, 180);
            g2.setColor(ModernColors.primaryWithAlpha(16));
            g2.fillOval(-50, height - 110, 210, 150);

            g2.setColor(new Color(255, 255, 255, 34));
            g2.fillRoundRect(54, 38, Math.max(220, width - 170), 24, 14, 14);
            g2.fillRoundRect(54, 76, Math.max(180, width - 250), 18, 12, 12);
            g2.fillRoundRect(54, 108, Math.max(210, width - 210), 18, 12, 12);
            g2.fillRoundRect(54, 140, Math.max(160, width - 300), 16, 10, 10);

            g2.setColor(new Color(255, 255, 255, 118));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, width - 5, height - 11, 28, 28);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

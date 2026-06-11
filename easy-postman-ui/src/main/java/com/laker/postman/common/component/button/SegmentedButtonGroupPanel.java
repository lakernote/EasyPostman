package com.laker.postman.common.component.button;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class SegmentedButtonGroupPanel extends JPanel {
    private static final int ARC = 10;

    public SegmentedButtonGroupPanel(int alignment) {
        super(new FlowLayout(alignment, 1, 2));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();
        if (width <= 1 || height <= 1) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(SegmentedButtonTheme.segmentBackground());
        g2.fillRoundRect(0, 0, width - 1, height - 1, ARC, ARC);
        g2.setColor(SegmentedButtonTheme.segmentBorder());
        g2.drawRoundRect(0, 0, width - 1, height - 1, ARC, ARC);
        g2.dispose();
    }
}

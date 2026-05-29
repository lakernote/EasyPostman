package com.laker.postman.common.component.button;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
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
        g2.setColor(SegmentedButtonTheme.segmentBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.setColor(SegmentedButtonTheme.segmentBorder());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.dispose();
        super.paintComponent(g);
    }
}

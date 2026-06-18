package com.laker.postman.common.component.button;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class SegmentedButtonGroupPanel extends JPanel {
    private static final int DEFAULT_ARC = 10;
    private final int arc;

    public SegmentedButtonGroupPanel(int alignment) {
        this(alignment, 1, 2, new Insets(2, 3, 2, 3), DEFAULT_ARC);
    }

    public static SegmentedButtonGroupPanel compact(int alignment) {
        return new SegmentedButtonGroupPanel(alignment, 0, 0, new Insets(2, 2, 2, 2), 8);
    }

    SegmentedButtonGroupPanel(int alignment, int hgap, int vgap, Insets borderInsets, int arc) {
        super(new FlowLayout(alignment, hgap, vgap));
        this.arc = arc;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(
                borderInsets.top,
                borderInsets.left,
                borderInsets.bottom,
                borderInsets.right
        ));
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
        RoundRectangle2D.Float segmentShape = new RoundRectangle2D.Float(
                0.5f, 0.5f, width - 1f, height - 1f, arc, arc);
        g2.setColor(SegmentedButtonTheme.segmentBackground());
        g2.fill(segmentShape);
        g2.setColor(SegmentedButtonTheme.segmentBorder());
        g2.draw(segmentShape);
        g2.dispose();
    }
}

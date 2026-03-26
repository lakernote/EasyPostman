package com.laker.postman.common.component.placeholder;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;

abstract class AbstractPlaceholderPanel extends JPanel {

    protected static final int DEFAULT_BLOCK_ARC = 12;

    protected AbstractPlaceholderPanel(LayoutManager layout) {
        super(layout);
    }

    protected void configureRoot(Color background, Insets insets) {
        setOpaque(true);
        setBackground(background);
        setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right));
    }

    protected JPanel createTransparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    protected JPanel createCardPanel(LayoutManager layout, Color background, Color borderColor, Insets insets) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)
        ));
        return panel;
    }

    protected JComponent createBlock(int width, int height, Color fillColor) {
        return new PlaceholderBlock(width, height, fillColor, DEFAULT_BLOCK_ARC);
    }

    protected JPanel createColumn(int[][] rows, Color fillColor, int verticalGap) {
        JPanel column = createTransparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (int i = 0; i < rows.length; i++) {
            column.add(createBlock(rows[i][0], rows[i][1], fillColor));
            if (i < rows.length - 1) {
                column.add(Box.createVerticalStrut(verticalGap));
            }
        }
        column.add(Box.createVerticalGlue());
        return column;
    }

    protected boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    protected void enableAntialias(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    protected void fillRoundedBlock(Graphics2D g2, int x, int y, int width, int height, Color color, int arc) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, width, height, arc, arc);
    }
}

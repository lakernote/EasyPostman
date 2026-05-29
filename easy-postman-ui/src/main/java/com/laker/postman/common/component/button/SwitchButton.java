package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Compact switch-style toggle button for settings panels.
 */
public class SwitchButton extends JToggleButton {
    private static final int DEFAULT_WIDTH = 35;
    private static final int DEFAULT_HEIGHT = 18;
    private static final Color TRACK_OFF = new Color(196, 201, 208);
    private static final Color TRACK_ON = ModernColors.PRIMARY;
    private static final Color THUMB = Color.WHITE;

    private final int trackWidth;
    private final int trackHeight;

    public SwitchButton() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public SwitchButton(int trackWidth, int trackHeight) {
        this.trackWidth = trackWidth;
        this.trackHeight = trackHeight;

        setModel(new ToggleButtonModel());
        setPreferredSize(new Dimension(trackWidth, trackHeight));
        setMinimumSize(new Dimension(trackWidth, trackHeight));
        setBorder(new EmptyBorder(0, 0, 0, 0));
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false);
        setRolloverEnabled(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int y = (getHeight() - trackHeight) / 2;
        int arc = trackHeight;
        Color trackColor = isSelected() ? TRACK_ON : TRACK_OFF;
        if (!isEnabled()) {
            trackColor = ModernColors.getBorderMediumColor();
        }

        g2.setColor(trackColor);
        g2.fillRoundRect(0, y, trackWidth, trackHeight, arc, arc);

        int thumbSize = trackHeight - 6;
        int thumbX = isSelected() ? trackWidth - thumbSize - 3 : 3;
        int thumbY = y + 3;
        g2.setColor(THUMB);
        g2.fillOval(thumbX, thumbY, thumbSize, thumbSize);
        g2.dispose();
    }
}

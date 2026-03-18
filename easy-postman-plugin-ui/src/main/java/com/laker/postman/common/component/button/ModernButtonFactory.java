package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared modern button styling used across dialogs and settings panels.
 */
public final class ModernButtonFactory {

    private static final int ARC = 8;
    private static final Dimension DEFAULT_SIZE = new Dimension(100, 34);
    private static final Border EMPTY_TEXT_PADDING = new EmptyBorder(7, 16, 7, 16);

    private ModernButtonFactory() {
    }

    public static JButton createButton(String text, boolean primary) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                paintModernButtonBackground(this, g, primary);
                super.paintComponent(g);
            }
        };
        configureBaseButton(button, primary);
        return button;
    }

    public static JToggleButton createToggleButton(String text) {
        JToggleButton button = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                paintModernButtonBackground(this, g, isSelected());
                super.paintComponent(g);
            }
        };
        configureBaseButton(button, false);
        return button;
    }

    private static void configureBaseButton(AbstractButton button, boolean primary) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        button.setForeground(primary ? ModernColors.getTextInverse() : ModernColors.getTextPrimary());
        button.setPreferredSize(DEFAULT_SIZE);
        button.setBorder(EMPTY_TEXT_PADDING);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });
    }

    private static void paintModernButtonBackground(AbstractButton button, Graphics g, boolean primary) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(resolveBackground(button, primary));
        g2.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), ARC, ARC);

        if (!primary) {
            g2.setColor(button.isEnabled() ? ModernColors.getBorderMediumColor() : ModernColors.getBorderLightColor());
            g2.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, ARC, ARC);
        }

        button.setForeground(primary ? ModernColors.getTextInverse() : ModernColors.getTextPrimary());
        g2.dispose();
    }

    private static Color resolveBackground(AbstractButton button, boolean primary) {
        ButtonModel model = button.getModel();

        if (primary) {
            if (!button.isEnabled()) {
                return ModernColors.getTextDisabled();
            }
            if (model.isPressed()) {
                return ModernColors.PRIMARY_DARKER;
            }
            if (model.isRollover()) {
                return ModernColors.PRIMARY_DARK;
            }
            return ModernColors.PRIMARY;
        }

        if (!button.isEnabled()) {
            return ModernColors.getBackgroundColor();
        }
        if (model.isPressed()) {
            return ModernColors.getButtonPressedColor();
        }
        if (model.isRollover()) {
            return ModernColors.getHoverBackgroundColor();
        }
        return ModernColors.getCardBackgroundColor();
    }
}

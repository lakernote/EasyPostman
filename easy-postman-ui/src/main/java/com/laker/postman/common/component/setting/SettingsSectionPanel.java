package com.laker.postman.common.component.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Shared settings section container with title and optional wrapping description.
 */
public class SettingsSectionPanel extends JPanel {

    public static final int DEFAULT_DESCRIPTION_WIDTH = 640;

    public SettingsSectionPanel(String title, String description) {
        this(title, description, DEFAULT_DESCRIPTION_WIDTH);
    }

    public SettingsSectionPanel(String title, String description, int descriptionWidth) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ModernColors.getCardBackgroundColor());
        setBorder(new CompoundBorder(
                new RoundedCardBorder(),
                new EmptyBorder(12, 12, 12, 12)
        ));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Short.MAX_VALUE, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (description == null || description.isEmpty()) {
            titleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
            add(titleLabel);
            return;
        }

        SettingsHintLabel descriptionLabel = new SettingsHintLabel(description, descriptionWidth);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        add(titleLabel);
        add(descriptionLabel);
    }

    private static final class RoundedCardBorder extends AbstractBorder {
        private static final int RADIUS = 8;
        private static final int SHADOW_SIZE = 4;

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                for (int i = SHADOW_SIZE; i > 0; i--) {
                    int alpha = (int) (8 * (1 - (double) i / SHADOW_SIZE));
                    g2.setColor(ModernColors.getShadowColor(alpha));
                    g2.fillRoundRect(x + i, y + i, width - i * 2, height - i * 2,
                            RADIUS + 2, RADIUS + 2);
                }

                g2.setColor(ModernColors.getCardBackgroundColor());
                g2.fillRoundRect(x + 1, y + 1, width - 2, height - 2, RADIUS, RADIUS);

                g2.setColor(ModernColors.getBorderLightColor());
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, RADIUS, RADIUS);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = SHADOW_SIZE;
            return insets;
        }
    }
}

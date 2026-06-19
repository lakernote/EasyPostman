package com.laker.postman.panel.update;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

@UtilityClass
class UpdateNotificationStyle {
    static final int NOTIFICATION_WIDTH = 392;
    private static final int INDICATOR_WIDTH = 4;
    private static final int CARD_ARC = 8;
    private static final int DESCRIPTION_WIDTH = 260;

    JPanel createRootPanel(Color indicatorColor) {
        NotificationCardPanel root = new NotificationCardPanel(indicatorColor);
        root.setLayout(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14 + INDICATOR_WIDTH, 14, 12));
        return root;
    }

    void setHoverAlpha(JPanel panel, int hoverAlpha) {
        if (panel instanceof NotificationCardPanel cardPanel) {
            cardPanel.setHoverAlpha(hoverAlpha);
        }
    }

    JLabel createIconLabel(String iconPath, Color indicatorColor) {
        JLabel iconLabel = new JLabel(IconUtil.createColored(iconPath, 26, 26, indicatorColor));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        return iconLabel;
    }

    JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        return contentPanel;
    }

    JLabel createTitleLabel(String text) {
        JLabel titleLabel = new JLabel(text);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return titleLabel;
    }

    JLabel createVersionLabel(String text) {
        JLabel versionLabel = new JLabel(text);
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        versionLabel.setForeground(ModernColors.getTextSecondary());
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return versionLabel;
    }

    JLabel createDescriptionLabel(String plainText) {
        JLabel descLabel = new JLabel("<html><body style='width:" + DESCRIPTION_WIDTH + "px'>"
                + UpdateTextFormatter.escapeHtml(plainText) + "</body></html>");
        descLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        descLabel.setForeground(ModernColors.getTextHint());
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return descLabel;
    }

    JButton createCloseButton() {
        JButton button = new JButton(closeIcon(ModernColors.getTextHint()));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
        button.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setIcon(closeIcon(ModernColors.getTextSecondary()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setIcon(closeIcon(ModernColors.getTextHint()));
            }
        });
        return button;
    }

    JButton createLinkButton(String text, Color foreground) {
        String color = ModernColors.toHtmlColor(foreground);
        JButton button = new JButton("<html><span style='color:" + color + "'><u>"
                + UpdateTextFormatter.escapeHtml(text) + "</u></span></html>");
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.putClientProperty(FlatClientProperties.STYLE, "borderWidth: 0; focusWidth: 0; innerFocusWidth: 0");
        return button;
    }

    private Icon closeIcon(Color color) {
        return IconUtil.createColored("icons/close.svg", 14, 14, color);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class NotificationCardPanel extends JPanel {
        private final Color indicatorColor;
        private int hoverAlpha;

        private void setHoverAlpha(int hoverAlpha) {
            this.hoverAlpha = hoverAlpha;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape card = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f,
                    CARD_ARC, CARD_ARC);
            g2.setClip(card);
            g2.setColor(ModernColors.getNotificationBackground());
            g2.fill(card);
            if (hoverAlpha > 0) {
                g2.setColor(ModernColors.withAlpha(ModernColors.getSelectionBackgroundColor(), hoverAlpha));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setColor(indicatorColor);
            g2.fillRect(0, 0, INDICATOR_WIDTH, getHeight());
            g2.setClip(null);
            g2.setColor(ModernColors.withAlpha(ModernColors.getNotificationBorder(),
                    ModernColors.isDarkTheme() ? 150 : 95));
            g2.draw(card);
            g2.dispose();
        }
    }
}

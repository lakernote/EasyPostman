package com.laker.postman.common.component.notification;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

@UtilityClass
class ToastStyle {
    static final int MIN_WIDTH = 280;
    static final int MAX_WIDTH = 360;
    static final int CORNER_RADIUS = 8;
    static final int HEADER_HEIGHT = 30;
    static final int ICON_SIZE = 18;
    static final int CLOSE_BUTTON_SIZE = 26;
    static final int HORIZONTAL_PADDING = 14;
    static final int VERTICAL_PADDING = 10;
    private static final int CONTENT_GAP = 10;
    private static final int TITLE_BODY_GAP = 3;
    private static final int BODY_ACTION_GAP = 6;
    static final int COLLAPSED_MAX_LINES = 4;
    static final int COLLAPSED_MAX_LENGTH = 120;
    static final int EXPANDED_MAX_BODY_HEIGHT = 240;
    static final int STACK_GAP = 6;
    static final int SLIDE_STEPS = 14;
    static final int SLIDE_INTERVAL = 14;
    static final int FADE_STEPS = 10;
    static final int FADE_INTERVAL = 18;

    static int bodyWidth() {
        return bodyWidth(MAX_WIDTH);
    }

    static int bodyWidth(int toastWidth) {
        return Math.max(1, toastWidth - HORIZONTAL_PADDING * 2
                - ICON_SIZE
                - CLOSE_BUTTON_SIZE
                - CONTENT_GAP * 2);
    }

    static int preferredWidth(String title, String message) {
        return preferredWidth(title, message, titleFont(), bodyFont());
    }

    static int preferredWidth(String title, String message, Font titleFont, Font bodyFont) {
        int textWidth = Math.max(maxLineWidth(title, titleFont), maxLineWidth(message, bodyFont));
        int width = textWidth
                + HORIZONTAL_PADDING * 2
                + ICON_SIZE
                + CLOSE_BUTTON_SIZE
                + CONTENT_GAP * 2;
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
    }

    static Color surfaceColor() {
        return ModernColors.getNotificationBackground();
    }

    static Color titleColor(NotificationCenter.NotificationType type) {
        return type.getColor();
    }

    static Color headerBackgroundColor(NotificationCenter.NotificationType type) {
        float ratio = ModernColors.isDarkTheme() ? 0.14f : 0.07f;
        return ModernColors.blendColors(surfaceColor(), type.getColor(), ratio);
    }

    static Color borderColor() {
        return ModernColors.withAlpha(ModernColors.getNotificationBorder(), ModernColors.isDarkTheme() ? 170 : 125);
    }

    static JPanel createCardPanel() {
        return createCardPanel(null);
    }

    static JPanel createCardPanel(NotificationCenter.NotificationType ignoredType) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                int width = getWidth();
                int height = getHeight();
                Shape card = new RoundRectangle2D.Float(0, 0, width, height,
                        CORNER_RADIUS, CORNER_RADIUS);
                g2.setColor(surfaceColor());
                g2.fill(card);
                g2.setColor(borderColor());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f,
                        CORNER_RADIUS, CORNER_RADIUS));
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return wrapper;
    }

    static JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(CONTENT_GAP, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(
                VERTICAL_PADDING,
                HORIZONTAL_PADDING,
                VERTICAL_PADDING,
                HORIZONTAL_PADDING
        ));
        return panel;
    }

    static JPanel createTextPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, TITLE_BODY_GAP));
        panel.setOpaque(false);
        return panel;
    }

    static JPanel createBodyPanel(JComponent bodyComponent, JComponent actionPanel) {
        JPanel panel = new JPanel(new BorderLayout(0, BODY_ACTION_GAP));
        panel.setOpaque(false);
        panel.add(bodyComponent, BorderLayout.CENTER);
        if (actionPanel != null) {
            panel.add(actionPanel, BorderLayout.SOUTH);
        }
        return panel;
    }

    static JPanel createClosePanel(JButton closeButton) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(closeButton, BorderLayout.NORTH);
        return panel;
    }

    static JLabel createTitleLabel(String title, NotificationCenter.NotificationType type) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(titleColor(type));
        titleLabel.setFont(titleFont());
        return titleLabel;
    }

    static JPanel createHeaderPanel(NotificationCenter.NotificationType type) {
        JPanel header = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                int width = getWidth();
                int height = getHeight();

                g2.setColor(headerBackgroundColor(type));
                g2.setClip(new RoundRectangle2D.Float(0, 0, width, height + CORNER_RADIUS,
                        CORNER_RADIUS, CORNER_RADIUS));
                g2.fillRect(0, 0, width, height);
                g2.setClip(null);

                g2.setColor(ModernColors.withAlpha(ModernColors.getNotificationDivider(),
                        ModernColors.isDarkTheme() ? 150 : 105));
                g2.drawLine(0, height - 1, width, height - 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setBorder(BorderFactory.createEmptyBorder(0, HORIZONTAL_PADDING, 0, 4));
        return header;
    }

    static JLabel createTypeIcon(NotificationCenter.NotificationType type) {
        JLabel iconLabel = new JLabel(type.getIcon()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                int size = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setColor(type.getColor());
                g2.fillOval(x, y, size, size);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLabel.setForeground(ModernColors.getTextInverse());
        iconLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        return iconLabel;
    }

    static JTextArea createBodyTextArea(String text) {
        JTextArea body = new JTextArea(text);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFocusable(false);
        body.setOpaque(false);
        body.setBorder(null);
        body.setForeground(ModernColors.getNotificationBodyForeground());
        body.setFont(bodyFont());
        return body;
    }

    static JScrollPane createBodyScrollPane(JTextArea body) {
        JScrollPane scrollPane = new ToastBodyScrollPane(body);
        applyBodyScrollPaneChrome(scrollPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    static void applyBodyScrollPaneChrome(JScrollPane scrollPane) {
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    static JPanel createActionPanel(AbstractButton expandButton, AbstractButton copyButton) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.add(expandButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(copyButton);
        return panel;
    }

    static JButton createLinkButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setOpaque(false);
        button.setForeground(ModernColors.getPrimary());
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        button.addActionListener(e -> action.run());
        return button;
    }

    static JButton createCloseButton(Runnable onClose) {
        JButton button = new JButton(closeIcon(ModernColors.withAlpha(ModernColors.getTextHint(), 135)));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setIcon(closeIcon(ModernColors.getTextSecondary()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setIcon(closeIcon(ModernColors.getTextHint()));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onClose.run();
            }
        });
        return button;
    }

    static void showCloseButton(AbstractButton button) {
        button.setIcon(closeIcon(ModernColors.getTextSecondary()));
    }

    static void hideCloseButton(AbstractButton button) {
        button.setIcon(closeIcon(ModernColors.withAlpha(ModernColors.getTextHint(), 135)));
    }

    static Graphics2D createGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    private Icon closeIcon(Color color) {
        return IconUtil.createColored("icons/close.svg", 14, 14, color);
    }

    private Font titleFont() {
        return FontsUtil.getDefaultFont(Font.BOLD);
    }

    private Font bodyFont() {
        return FontsUtil.getDefaultFont(Font.PLAIN);
    }

    private int maxLineWidth(String text, Font font) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        FontMetrics metrics = new JLabel().getFontMetrics(font);
        int width = 0;
        for (String line : text.split("\n", -1)) {
            width = Math.max(width, metrics.stringWidth(line));
        }
        return width;
    }

    private static final class ToastBodyScrollPane extends JScrollPane {
        private ToastBodyScrollPane(JTextArea body) {
            super(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            ToastStyle.applyBodyScrollPaneChrome(this);
        }
    }
}

package com.laker.postman.panel.update;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@UtilityClass
final class UpdateDialogStyle {
    static final int UPDATE_DIALOG_MIN_WIDTH = 720;
    static final int UPDATE_DIALOG_MIN_HEIGHT = 330;
    static final int UPDATE_DIALOG_DEFAULT_HEIGHT = 360;
    static final int CHANGELOG_PREFERRED_HEIGHT = 106;
    static final int NO_ASSET_DIALOG_MIN_WIDTH = 640;
    static final int NO_ASSET_DIALOG_MIN_HEIGHT = 300;
    static final int NO_ASSET_DIALOG_DEFAULT_HEIGHT = 320;
    private static final int BUTTON_HEIGHT = 34;
    private static final int MIN_BUTTON_WIDTH = 100;

    static JPanel createHeaderPanel(String iconPath, Color accent, String title, String versionText, String dateText) {
        JPanel panel = new AccentHeaderPanel(accent);
        panel.setLayout(new BorderLayout(14, 0));
        panel.setOpaque(true);
        panel.setBackground(ModernColors.getDialogChromeBackgroundColor());
        ToolWindowSurfaceStyle.applyDialogHeader(panel, 14, 22, 14, 22);

        JPanel badgeWrapper = new JPanel(new GridBagLayout());
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(new IconBadge(iconPath, accent));
        panel.add(badgeWrapper, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel versionLabel = new JLabel(versionText);
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        versionLabel.setForeground(accent);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(versionLabel);

        if (dateText != null && !dateText.isBlank()) {
            JLabel dateLabel = new JLabel(dateText);
            dateLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            dateLabel.setForeground(ModernColors.getTextHint());
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(Box.createVerticalStrut(4));
            textPanel.add(dateLabel);
        }

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    static JLabel createSectionTitle(String text) {
        JLabel titleLabel = new JLabel(text);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        return titleLabel;
    }

    static JScrollPane createFramedReadOnlyScrollPane(JTextArea textArea) {
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        textArea.setBorder(new EmptyBorder(10, 12, 12, 12));
        textArea.setCaretPosition(0);
        ToolWindowSurfaceStyle.applyTextComponentDialogSurface(textArea);

        JScrollPane scrollPane = new JScrollPane(textArea);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        ToolWindowSurfaceStyle.applyDialogFrame(scrollPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    static JLabel createHtmlBodyLabel(String bodyHtml, int width) {
        JLabel label = new JLabel("<html><body style='width:" + width + "px'>" + bodyHtml + "</body></html>");
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        label.setForeground(ModernColors.getTextSecondary());
        return label;
    }

    static JPanel createFooterTip(String text) {
        JPanel tipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(tipPanel);
        tipPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel iconLabel = new JLabel(IconUtil.createColored("icons/info.svg", 14, 14, ModernColors.getWarning()));
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        textLabel.setForeground(ModernColors.getTextHint());

        tipPanel.add(iconLabel);
        tipPanel.add(textLabel);
        return tipPanel;
    }

    static JPanel createTipBar(String text, Color accent, int width) {
        JPanel bar = new TipBarPanel(accent);
        bar.setLayout(new BorderLayout(8, 0));
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));
        bar.setOpaque(false);

        JLabel iconLabel = new JLabel(IconUtil.createColored("icons/warning.svg", 16, 16, accent));
        JLabel tipLabel = createHtmlBodyLabel(text, width);
        tipLabel.setForeground(ModernColors.getTextSecondary());

        bar.add(iconLabel, BorderLayout.WEST);
        bar.add(tipLabel, BorderLayout.CENTER);
        return bar;
    }

    static JButton createPrimaryButton(String text) {
        return createPrimaryButton(text, null);
    }

    static JButton createPrimaryButton(String text, String iconPath) {
        JButton button = iconPath == null
                ? ModernButtonFactory.createButton(text, true)
                : ModernButtonFactory.createButton(text, true, iconPath);
        fitButtonToText(button);
        return button;
    }

    static JButton createSecondaryButton(String text) {
        JButton button = ModernButtonFactory.createButton(text, false);
        fitButtonToText(button);
        return button;
    }

    static void installDefaultButton(JDialog dialog, JButton button) {
        dialog.getRootPane().setDefaultButton(button);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(button::requestFocusInWindow);
            }
        });
    }

    private static void fitButtonToText(JButton button) {
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        int textWidth = button.getText() == null ? 0 : metrics.stringWidth(button.getText());
        int iconWidth = button.getIcon() == null ? 0 : button.getIcon().getIconWidth() + button.getIconTextGap();
        int width = Math.max(MIN_BUTTON_WIDTH, textWidth + iconWidth + 34);
        Dimension size = new Dimension(width, BUTTON_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class AccentHeaderPanel extends JPanel {
        private final Color accent;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int washAlpha = ModernColors.isDarkTheme() ? 14 : 7;
            g2.setPaint(new GradientPaint(
                    0, 0, ModernColors.withAlpha(accent, washAlpha),
                    getWidth(), 0, ModernColors.withAlpha(accent, 0)
            ));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(ModernColors.withAlpha(accent, ModernColors.isDarkTheme() ? 105 : 82));
            g2.fillRoundRect(0, 0, 3, getHeight(), 3, 3);
            g2.dispose();
        }
    }

    private static final class IconBadge extends JComponent {
        private static final int SIZE = 42;
        private static final int ICON_SIZE = 22;
        private final Icon icon;
        private final Color accent;

        private IconBadge(String iconPath, Color accent) {
            this.icon = IconUtil.createColored(iconPath, ICON_SIZE, ICON_SIZE, accent);
            this.accent = accent;
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int padding = 2;
            g2.setColor(ModernColors.withAlpha(accent, ModernColors.isDarkTheme() ? 22 : 10));
            g2.fillOval(padding, padding, getWidth() - padding * 2, getHeight() - padding * 2);
            g2.setColor(ModernColors.withAlpha(accent, ModernColors.isDarkTheme() ? 72 : 46));
            g2.drawOval(padding, padding, getWidth() - padding * 2 - 1, getHeight() - padding * 2 - 1);

            int x = (getWidth() - icon.getIconWidth()) / 2;
            int y = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, x, y);
            g2.dispose();
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class TipBarPanel extends JPanel {
        private final Color accent;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = ModernColors.blendColors(
                    ModernColors.getDialogChromeBackgroundColor(),
                    accent,
                    ModernColors.isDarkTheme() ? 0.16f : 0.08f
            );
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(ModernColors.withAlpha(accent, ModernColors.isDarkTheme() ? 125 : 145));
            g2.fillRoundRect(0, 0, 3, getHeight(), 3, 3);
            g2.dispose();
        }
    }
}

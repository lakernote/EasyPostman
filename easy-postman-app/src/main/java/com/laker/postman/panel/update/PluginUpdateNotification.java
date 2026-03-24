package com.laker.postman.panel.update;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.update.plugin.PluginUpdateCandidate;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * 插件更新通知弹窗。
 */
@Slf4j
public class PluginUpdateNotification {

    private static final int NOTIFICATION_WIDTH = 420;
    private static final int MARGIN = 20;
    private static final int FADE_DURATION = 300;
    private static final int DISPLAY_DURATION = 7000;
    private static final float FADE_STEP = 0.05f;
    private static final int FADE_TIMER_DELAY = 10;
    private static final int INDICATOR_WIDTH = 4;
    private static final int BOTTOM_CLEARANCE = 40;

    private final JDialog dialog;
    private final JFrame parent;
    private Timer fadeTimer;
    private Timer autoCloseTimer;
    private float opacity = 0f;
    private ComponentAdapter parentMoveListener;
    private int hoverAlpha = 0;

    private PluginUpdateNotification(JFrame parent,
                                     List<PluginUpdateCandidate> updates,
                                     Consumer<List<PluginUpdateCandidate>> onAction) {
        this.parent = parent;
        this.dialog = new JDialog(parent, false);
        dialog.setUndecorated(true);
        dialog.setFocusableWindowState(false);
        dialog.setType(Window.Type.UTILITY);
        dialog.setOpacity(0f);

        JPanel contentPanel = createNotificationPanel(updates, onAction);
        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setSize(NOTIFICATION_WIDTH, dialog.getHeight());

        positionDialog();
        registerParentMoveListener();
    }

    public static void show(JFrame parent,
                            List<PluginUpdateCandidate> updates,
                            Consumer<List<PluginUpdateCandidate>> onAction) {
        SwingUtilities.invokeLater(() -> {
            if (parent == null || !parent.isVisible() || updates == null || updates.isEmpty()) {
                return;
            }
            new PluginUpdateNotification(parent, updates, onAction).display();
        });
    }

    private void display() {
        dialog.setVisible(true);
        fadeIn();
        scheduleAutoClose();
    }

    private void scheduleAutoClose() {
        stopAutoCloseTimer();
        autoCloseTimer = new Timer(DISPLAY_DURATION, e -> fadeOut());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    private void fadeIn() {
        stopFadeTimer();
        fadeTimer = new Timer(FADE_TIMER_DELAY, null);
        fadeTimer.addActionListener(e -> {
            opacity = Math.min(opacity + FADE_STEP, 1.0f);
            dialog.setOpacity(opacity);
            if (opacity >= 1.0f) {
                stopFadeTimer();
            }
        });
        fadeTimer.start();
    }

    private void fadeOut() {
        stopFadeTimer();
        stopAutoCloseTimer();
        fadeTimer = new Timer(FADE_TIMER_DELAY, null);
        fadeTimer.addActionListener(e -> {
            opacity = Math.max(opacity - FADE_STEP, 0f);
            dialog.setOpacity(opacity);
            if (opacity <= 0f) {
                stopFadeTimer();
                cleanupAndClose();
            }
        });
        fadeTimer.start();
    }

    private void stopFadeTimer() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
    }

    private void stopAutoCloseTimer() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
            autoCloseTimer = null;
        }
    }

    private void cleanupAndClose() {
        stopFadeTimer();
        stopAutoCloseTimer();
        if (parentMoveListener != null) {
            parent.removeComponentListener(parentMoveListener);
            parentMoveListener = null;
        }
        dialog.dispose();
    }

    private void registerParentMoveListener() {
        parentMoveListener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                positionDialog();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                positionDialog();
            }
        };
        parent.addComponentListener(parentMoveListener);
    }

    private void positionDialog() {
        Rectangle parentBounds = parent.getBounds();
        int x = parentBounds.x + parentBounds.width - NOTIFICATION_WIDTH - MARGIN;
        int y = parentBounds.y + parentBounds.height - dialog.getHeight() - MARGIN - BOTTOM_CLEARANCE;

        GraphicsConfiguration graphicsConfiguration = parent.getGraphicsConfiguration();
        if (graphicsConfiguration != null) {
            Rectangle screen = graphicsConfiguration.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
            x = Math.min(x, screen.x + screen.width - insets.right - NOTIFICATION_WIDTH - MARGIN);
            y = Math.min(y, screen.y + screen.height - insets.bottom - dialog.getHeight() - MARGIN);
            x = Math.max(x, screen.x + insets.left + MARGIN);
            y = Math.max(y, screen.y + insets.top + MARGIN);
        }

        dialog.setLocation(x, y);
    }

    private JPanel createNotificationPanel(List<PluginUpdateCandidate> updates,
                                           Consumer<List<PluginUpdateCandidate>> onAction) {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIManager.getColor("Panel.background"));
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (hoverAlpha > 0) {
                    Color selectionColor = UIManager.getColor("List.selectionBackground");
                    if (selectionColor != null) {
                        g2.setColor(new Color(
                                selectionColor.getRed(),
                                selectionColor.getGreen(),
                                selectionColor.getBlue(),
                                hoverAlpha
                        ));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                }
                g2.setColor(ModernColors.INFO);
                g2.fillRect(0, 0, INDICATOR_WIDTH, getHeight());
                g2.setColor(ModernColors.getBorderLightColor());
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14 + INDICATOR_WIDTH, 14, 14));
        root.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                stopAutoCloseTimer();
                hoverAlpha = 30;
                root.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Component destination = SwingUtilities.getDeepestComponentAt(root, e.getX(), e.getY());
                if (destination != null && SwingUtilities.isDescendingFrom(destination, root)) {
                    return;
                }
                hoverAlpha = 0;
                root.repaint();
                scheduleAutoClose();
            }
        });

        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/info.svg", 32, 32));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        root.add(iconLabel, BorderLayout.WEST);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PLUGIN_UPDATE_NOTIFICATION_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel summaryLabel = new JLabel("<html><body style='width:240px'>" + buildSummaryHtml(updates) + "</body></html>");
        summaryLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        summaryLabel.setForeground(ModernColors.getTextSecondary());
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton actionButton = createActionButton(updates, onAction);
        actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(summaryLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(actionButton);
        root.add(contentPanel, BorderLayout.CENTER);

        JButton closeButton = createCloseButton();
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRight.setOpaque(false);
        topRight.add(closeButton);
        root.add(topRight, BorderLayout.EAST);

        return root;
    }

    private String buildSummaryHtml(List<PluginUpdateCandidate> updates) {
        PluginUpdateCandidate first = updates.get(0);
        if (updates.size() == 1) {
            return I18nUtil.getMessage(
                    MessageKeys.PLUGIN_UPDATE_NOTIFICATION_SINGLE,
                    escapeHtml(first.pluginName()),
                    escapeHtml(first.installedVersion()),
                    escapeHtml(first.latestVersion())
            );
        }

        StringBuilder html = new StringBuilder();
        html.append(I18nUtil.getMessage(
                MessageKeys.PLUGIN_UPDATE_NOTIFICATION_MULTIPLE,
                updates.size(),
                escapeHtml(first.pluginName()),
                escapeHtml(first.installedVersion()),
                escapeHtml(first.latestVersion())
        ));
        html.append("<br>");
        html.append(I18nUtil.getMessage(
                MessageKeys.PLUGIN_UPDATE_NOTIFICATION_MORE,
                updates.size() - 1
        ));
        return html.toString();
    }

    private JButton createActionButton(List<PluginUpdateCandidate> updates,
                                       Consumer<List<PluginUpdateCandidate>> onAction) {
        JButton button = new JButton(
                "<html><u>" + I18nUtil.getMessage(MessageKeys.PLUGIN_UPDATE_NOTIFICATION_ACTION) + "</u></html>");
        button.setForeground(ModernColors.INFO);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(e -> {
            fadeOut();
            Timer delayTimer = new Timer(FADE_DURATION, evt -> onAction.accept(updates));
            delayTimer.setRepeats(false);
            delayTimer.start();
        });
        return button;
    }

    private JButton createCloseButton() {
        JButton button = new JButton("×");
        button.setFont(new Font("Arial", Font.PLAIN, 18));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(22, 22));
        button.setMaximumSize(new Dimension(22, 22));
        button.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.addActionListener(e -> fadeOut());
        return button;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

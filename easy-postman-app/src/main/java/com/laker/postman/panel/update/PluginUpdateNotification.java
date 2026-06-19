package com.laker.postman.panel.update;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.update.plugin.PluginUpdateCandidate;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
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
    private static final int DISPLAY_DURATION = 7000;

    private final UpdateFloatingNotificationWindow window;

    private PluginUpdateNotification(JFrame parent,
                                     List<PluginUpdateCandidate> updates,
                                     Consumer<List<PluginUpdateCandidate>> onAction) {
        this.window = new UpdateFloatingNotificationWindow(parent, DISPLAY_DURATION);
        JPanel contentPanel = createNotificationPanel(updates, onAction);
        window.installContent(contentPanel, NOTIFICATION_WIDTH);
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
        window.display();
    }

    private JPanel createNotificationPanel(List<PluginUpdateCandidate> updates,
                                           Consumer<List<PluginUpdateCandidate>> onAction) {
        JPanel root = UpdateNotificationStyle.createRootPanel(ModernColors.getInfo());
        root.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                window.pauseAutoClose();
                UpdateNotificationStyle.setHoverAlpha(root, 30);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Component destination = SwingUtilities.getDeepestComponentAt(root, e.getX(), e.getY());
                if (destination != null && SwingUtilities.isDescendingFrom(destination, root)) {
                    return;
                }
                UpdateNotificationStyle.setHoverAlpha(root, 0);
                window.resumeAutoClose();
            }
        });

        root.add(UpdateNotificationStyle.createIconLabel("icons/info.svg", ModernColors.getInfo()), BorderLayout.WEST);

        JPanel contentPanel = UpdateNotificationStyle.createContentPanel();

        JLabel titleLabel = UpdateNotificationStyle.createTitleLabel(
                I18nUtil.getMessage(MessageKeys.PLUGIN_UPDATE_NOTIFICATION_TITLE)
        );

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

        JButton closeButton = UpdateNotificationStyle.createCloseButton();
        closeButton.addActionListener(e -> window.fadeOut());
        JPanel topRight = new JPanel(new BorderLayout());
        topRight.setOpaque(false);
        topRight.add(closeButton, BorderLayout.NORTH);
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
        JButton button = UpdateNotificationStyle.createLinkButton(
                I18nUtil.getMessage(MessageKeys.PLUGIN_UPDATE_NOTIFICATION_ACTION),
                ModernColors.getInfo()
        );
        button.addActionListener(e -> {
            window.fadeOutThen(() -> onAction.accept(updates));
        });
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

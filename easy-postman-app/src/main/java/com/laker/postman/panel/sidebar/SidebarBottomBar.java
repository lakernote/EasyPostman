package com.laker.postman.panel.sidebar;

import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.plugin.api.StatusBarActionContribution;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 侧边栏底部操作区，只负责组件创建和状态刷新。
 */
final class SidebarBottomBar {
    private static final String TOOLTIP_COLLAPSE_SIDEBAR = "Collapse sidebar";
    private static final String TOOLTIP_EXPAND_SIDEBAR = "Expand sidebar";
    private static final int BOTTOM_BAR_ICON_SIZE = 19;
    static final int BOTTOM_BAR_ACTION_SIZE = ToolWindowStripeMetrics.ACTION_SIZE;
    static final int STRIPE_THICKNESS = ToolWindowStripeMetrics.STRIPE_THICKNESS;
    private static final int BOTTOM_BAR_TEXT_VERTICAL_PADDING = 4;

    private final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private final JLabel consoleLabel;
    private final JLabel sidebarToggleLabel;
    private final JLabel layoutToggleLabel;
    private final JLabel globalVariablesLabel;
    private final JLabel cookieLabel;

    SidebarBottomBar(boolean sidebarExpanded,
                     Runnable toggleSidebarExpansionAction,
                     Runnable openConsoleAction,
                     Runnable toggleLayoutOrientationAction,
                     Runnable openGlobalVariablesAction,
                     Runnable openCookieManagerAction) {
        this(
                sidebarExpanded,
                toggleSidebarExpansionAction,
                openConsoleAction,
                toggleLayoutOrientationAction,
                openGlobalVariablesAction,
                openCookieManagerAction,
                List.of(),
                ignored -> {
                }
        );
    }

    SidebarBottomBar(boolean sidebarExpanded,
                     Runnable toggleSidebarExpansionAction,
                     Runnable openConsoleAction,
                     Runnable toggleLayoutOrientationAction,
                     Runnable openGlobalVariablesAction,
                     Runnable openCookieManagerAction,
                     List<StatusBarActionContribution> statusBarActions,
                     Consumer<StatusBarActionContribution> statusBarActionHandler) {
        leftPanel.setOpaque(false);
        rightPanel.setOpaque(false);

        sidebarToggleLabel = createActionLabel(
                null,
                IconUtil.createThemed("icons/sidebar-toggle.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                toggleSidebarExpansionAction
        );
        consoleLabel = createActionLabel(
                I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE),
                IconUtil.createThemed("icons/console.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                openConsoleAction
        );
        layoutToggleLabel = createActionLabel(
                null,
                layoutToggleIcon(SettingManager.isLayoutVertical()),
                toggleLayoutOrientationAction
        );
        globalVariablesLabel = createActionLabel(
                I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_TITLE),
                IconUtil.createThemed("icons/global-variables.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                openGlobalVariablesAction
        );
        cookieLabel = createActionLabel(
                I18nUtil.getMessage(MessageKeys.COOKIES_TITLE),
                IconUtil.createThemed("icons/cookie.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                openCookieManagerAction
        );
        JLabel versionLabel = createVersionLabel();

        leftPanel.add(sidebarToggleLabel);
        leftPanel.add(consoleLabel);
        rightPanel.add(versionLabel);
        addStatusBarActions(statusBarActions, statusBarActionHandler);
        rightPanel.add(globalVariablesLabel);
        rightPanel.add(cookieLabel);
        rightPanel.add(layoutToggleLabel);

        setSidebarExpanded(sidebarExpanded);
        updateLayoutToggleState(SettingManager.isLayoutVertical());
    }

    private void addStatusBarActions(List<StatusBarActionContribution> statusBarActions,
                                     Consumer<StatusBarActionContribution> statusBarActionHandler) {
        if (statusBarActions == null || statusBarActions.isEmpty()) {
            return;
        }
        statusBarActions.stream()
                .filter(this::isValidStatusBarAction)
                .sorted(Comparator
                        .comparingInt(StatusBarActionContribution::order)
                        .thenComparing(StatusBarActionContribution::id))
                .forEach(contribution -> rightPanel.add(createActionLabel(
                        contribution.tooltip(),
                        IconUtil.createThemed(
                                contribution.iconPath(),
                                BOTTOM_BAR_ICON_SIZE,
                                BOTTOM_BAR_ICON_SIZE,
                                contribution.iconClassLoader()
                        ),
                        () -> statusBarActionHandler.accept(contribution)
                )));
    }

    private boolean isValidStatusBarAction(StatusBarActionContribution contribution) {
        return contribution != null
                && !contribution.id().isBlank()
                && !contribution.tooltip().isBlank()
                && !contribution.iconPath().isBlank()
                && !contribution.targetType().isBlank()
                && !contribution.targetId().isBlank();
    }

    JPanel leftPanel() {
        return leftPanel;
    }

    JPanel rightPanel() {
        return rightPanel;
    }

    void setSidebarExpanded(boolean sidebarExpanded) {
        setActionLabelText(sidebarToggleLabel, sidebarExpanded ? TOOLTIP_COLLAPSE_SIDEBAR : TOOLTIP_EXPAND_SIDEBAR);
    }

    void updateLayoutToggleState(boolean isVertical) {
        layoutToggleLabel.setIcon(layoutToggleIcon(isVertical));
        setActionLabelText(layoutToggleLabel, isVertical
                ? I18nUtil.getMessage(MessageKeys.LAYOUT_HORIZONTAL_TOOLTIP)
                : I18nUtil.getMessage(MessageKeys.LAYOUT_VERTICAL_TOOLTIP));
    }

    void refreshLocalizedText() {
        setActionLabelText(consoleLabel, I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE));
        setActionLabelText(globalVariablesLabel, I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_TITLE));
        setActionLabelText(cookieLabel, I18nUtil.getMessage(MessageKeys.COOKIES_TITLE));
        updateLayoutToggleState(SettingManager.isLayoutVertical());
    }

    private JLabel createActionLabel(String tooltipText, Icon icon, Runnable action) {
        JLabel label = new JLabel(icon);
        setActionLabelText(label, tooltipText);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createEmptyBorder());
        Dimension size = ToolWindowStripeMetrics.actionSize();
        label.setPreferredSize(size);
        label.setMinimumSize(size);
        label.setMaximumSize(size);
        label.setFocusable(true);
        label.setEnabled(true);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                action.run();
            }
        });
        return label;
    }

    private void setActionLabelText(JLabel label, String text) {
        label.setToolTipText(text);
        label.getAccessibleContext().setAccessibleName(text);
    }

    private JLabel createVersionLabel() {
        JLabel versionLabel = new JLabel(SystemUtil.getCurrentVersion());
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        versionLabel.setBorder(BorderFactory.createEmptyBorder(
                BOTTOM_BAR_TEXT_VERTICAL_PADDING,
                4,
                BOTTOM_BAR_TEXT_VERTICAL_PADDING,
                8
        ));
        return versionLabel;
    }

    private Icon layoutToggleIcon(boolean isVertical) {
        String iconPath = isVertical ? "icons/layout-horizontal.svg" : "icons/layout-vertical.svg";
        return IconUtil.createThemed(iconPath, BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE);
    }
}

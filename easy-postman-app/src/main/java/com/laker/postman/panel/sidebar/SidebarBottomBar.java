package com.laker.postman.panel.sidebar;

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

/**
 * 侧边栏底部操作区，只负责组件创建和状态刷新。
 */
final class SidebarBottomBar {
    private static final String TOOLTIP_COLLAPSE_SIDEBAR = "Collapse sidebar";
    private static final String TOOLTIP_EXPAND_SIDEBAR = "Expand sidebar";
    private static final int BOTTOM_BAR_ICON_SIZE = 20;

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
        leftPanel.setOpaque(false);
        rightPanel.setOpaque(false);

        sidebarToggleLabel = createActionLabel(
                null,
                IconUtil.createThemed("icons/sidebar-toggle.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                8,
                4,
                toggleSidebarExpansionAction
        );
        consoleLabel = createActionLabel(
                I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE),
                IconUtil.createThemed("icons/console.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                4,
                4,
                openConsoleAction
        );
        layoutToggleLabel = createActionLabel(
                null,
                layoutToggleIcon(SettingManager.isLayoutVertical()),
                4,
                12,
                toggleLayoutOrientationAction
        );
        globalVariablesLabel = createActionLabel(
                I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_TITLE),
                IconUtil.createThemed("icons/global-variables.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                4,
                4,
                openGlobalVariablesAction
        );
        cookieLabel = createActionLabel(
                I18nUtil.getMessage(MessageKeys.COOKIES_TITLE),
                IconUtil.create("icons/cookie.svg", BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE),
                4,
                4,
                openCookieManagerAction
        );
        JLabel versionLabel = createVersionLabel();

        leftPanel.add(sidebarToggleLabel);
        leftPanel.add(consoleLabel);
        rightPanel.add(versionLabel);
        rightPanel.add(globalVariablesLabel);
        rightPanel.add(cookieLabel);
        rightPanel.add(layoutToggleLabel);

        setSidebarExpanded(sidebarExpanded);
        updateLayoutToggleState(SettingManager.isLayoutVertical());
    }

    JPanel leftPanel() {
        return leftPanel;
    }

    JPanel rightPanel() {
        return rightPanel;
    }

    void setSidebarExpanded(boolean sidebarExpanded) {
        sidebarToggleLabel.setToolTipText(sidebarExpanded ? TOOLTIP_COLLAPSE_SIDEBAR : TOOLTIP_EXPAND_SIDEBAR);
    }

    void updateLayoutToggleState(boolean isVertical) {
        layoutToggleLabel.setIcon(layoutToggleIcon(isVertical));
        layoutToggleLabel.setToolTipText(isVertical
                ? I18nUtil.getMessage(MessageKeys.LAYOUT_HORIZONTAL_TOOLTIP)
                : I18nUtil.getMessage(MessageKeys.LAYOUT_VERTICAL_TOOLTIP));
    }

    void refreshLocalizedText() {
        consoleLabel.setText(I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE));
        globalVariablesLabel.setText(I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_TITLE));
        cookieLabel.setText(I18nUtil.getMessage(MessageKeys.COOKIES_TITLE));
        updateLayoutToggleState(SettingManager.isLayoutVertical());
    }

    private JLabel createActionLabel(String text, Icon icon, int leftPadding, int rightPadding, Runnable action) {
        JLabel label = text == null ? new JLabel(icon) : new JLabel(text);
        if (text != null) {
            label.setIcon(icon);
        }
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createEmptyBorder(4, leftPadding, 4, rightPadding));
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

    private JLabel createVersionLabel() {
        JLabel versionLabel = new JLabel(SystemUtil.getCurrentVersion());
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        versionLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return versionLabel;
    }

    private Icon layoutToggleIcon(boolean isVertical) {
        String iconPath = isVertical ? "icons/layout-horizontal.svg" : "icons/layout-vertical.svg";
        return IconUtil.createThemed(iconPath, BOTTOM_BAR_ICON_SIZE, BOTTOM_BAR_ICON_SIZE);
    }
}

package com.laker.postman.panel.topmenu;

import com.laker.postman.panel.topmenu.setting.ModernSettingsDialog;
import com.laker.postman.panel.topmenu.setting.SettingsContribution;
import com.laker.postman.panel.topmenu.setting.SettingsContributionRegistry;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

/**
 * 顶部设置菜单。
 */
@UtilityClass
class TopMenuSettingsMenu {

    JMenu create(Component parent) {
        SettingsContributionRegistry settingsRegistry = SettingsContributionRegistry.defaultRegistry();
        JMenu settingMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_SETTINGS));

        JMenuItem settingsMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_TITLE));
        settingsMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_COMMA,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
        ));
        settingsMenuItem.addActionListener(e -> showSettingsDialog(parent));
        settingMenu.add(settingsMenuItem);

        settingMenu.addSeparator();
        int tabIndex = 0;
        for (SettingsContribution contribution : settingsRegistry.contributions()) {
            settingMenu.add(createTabItem(parent, contribution, tabIndex++));
        }

        return settingMenu;
    }

    private JMenuItem createTabItem(Component parent, SettingsContribution contribution, int tabIndex) {
        JMenuItem menuItem = new JMenuItem(contribution.resolveTitle());
        menuItem.addActionListener(e -> showSettingsDialog(parent, tabIndex));
        return menuItem;
    }

    private void showSettingsDialog(Component parent) {
        ModernSettingsDialog.showSettings(SwingUtilities.getWindowAncestor(parent));
    }

    private void showSettingsDialog(Component parent, int tabIndex) {
        ModernSettingsDialog.showSettings(SwingUtilities.getWindowAncestor(parent), tabIndex);
    }
}

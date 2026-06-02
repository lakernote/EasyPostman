package com.laker.postman.panel.topmenu;

import com.laker.postman.panel.topmenu.setting.ModernSettingsDialog;
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
        JMenu settingMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_SETTINGS));

        JMenuItem settingsMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_TITLE));
        settingsMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_COMMA,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
        ));
        settingsMenuItem.addActionListener(e -> showSettingsDialog(parent));
        settingMenu.add(settingsMenuItem);

        settingMenu.addSeparator();
        settingMenu.add(createTabItem(parent, MessageKeys.SETTINGS_GENERAL_TITLE, 0));
        settingMenu.add(createTabItem(parent, MessageKeys.SETTINGS_REQUEST_TITLE, 1));
        settingMenu.add(createTabItem(parent, MessageKeys.SETTINGS_PROXY_TITLE, 2));
        settingMenu.add(createTabItem(parent, MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_TITLE, 3));
        settingMenu.add(createTabItem(parent, MessageKeys.SETTINGS_AUTO_UPDATE_TITLE, 4));
        settingMenu.add(createTabItem(parent, MessageKeys.SETTINGS_PERFORMANCE_TITLE, 5));
        settingMenu.add(createTabItem(parent, MessageKeys.CERT_TITLE, 6));
        settingMenu.add(createTabItem(parent, MessageKeys.SETTINGS_SHORTCUTS_TITLE, 7));

        return settingMenu;
    }

    private JMenuItem createTabItem(Component parent, String messageKey, int tabIndex) {
        JMenuItem menuItem = new JMenuItem(I18nUtil.getMessage(messageKey));
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

package com.laker.postman.panel.topmenu;

import com.laker.postman.panel.topmenu.plugin.PluginManagerDialog;
import com.laker.postman.plugin.manager.PluginManagementService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Desktop;

/**
 * 顶部插件菜单。
 */
@Slf4j
@UtilityClass
class TopMenuPluginMenu {

    JMenu create(Component parent) {
        JMenu pluginMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_PLUGINS));

        JMenuItem pluginCenterMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_PLUGINS_CENTER));
        pluginCenterMenuItem.addActionListener(e -> showPluginManagerDialog(parent));
        pluginMenu.add(pluginCenterMenuItem);

        JMenuItem openPluginFolderItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_PLUGINS_OPEN_DIR));
        openPluginFolderItem.addActionListener(e -> openPluginDirectory(parent));
        pluginMenu.add(openPluginFolderItem);

        return pluginMenu;
    }

    private void showPluginManagerDialog(Component parent) {
        PluginManagerDialog.showDialog(SwingUtilities.getWindowAncestor(parent));
    }

    private void openPluginDirectory(Component parent) {
        try {
            Desktop.getDesktop().open(PluginManagementService.getManagedPluginDir().toFile());
        } catch (Exception e) {
            log.error("Failed to open plugin directory", e);
            JOptionPane.showMessageDialog(parent,
                    e.getMessage(),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

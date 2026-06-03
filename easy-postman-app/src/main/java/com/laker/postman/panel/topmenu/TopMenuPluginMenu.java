package com.laker.postman.panel.topmenu;

import com.laker.postman.panel.topmenu.plugin.PluginManagerDialog;
import com.laker.postman.plugin.api.PluginMenuActionContext;
import com.laker.postman.plugin.api.PluginMenuContribution;
import com.laker.postman.plugin.host.PluginAccess;
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
import java.awt.Window;
import java.util.Comparator;
import java.util.List;

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

        addPluginMenuContributions(pluginMenu, parent);

        return pluginMenu;
    }

    private void addPluginMenuContributions(JMenu pluginMenu, Component parent) {
        List<PluginMenuContribution> contributions = PluginAccess.getMenuContributions().stream()
                .filter(contribution -> PluginMenuContribution.PARENT_MENU_PLUGINS.equals(contribution.parentMenuId()))
                .sorted(Comparator.comparingInt(PluginMenuContribution::order).thenComparing(PluginMenuContribution::id))
                .toList();
        if (contributions.isEmpty()) {
            return;
        }

        pluginMenu.addSeparator();
        for (PluginMenuContribution contribution : contributions) {
            pluginMenu.add(createPluginMenuItem(parent, contribution));
        }
    }

    private JMenuItem createPluginMenuItem(Component parent, PluginMenuContribution contribution) {
        JMenuItem menuItem = new JMenuItem(resolveTitle(contribution));
        menuItem.addActionListener(e -> runPluginMenuAction(parent, contribution));
        return menuItem;
    }

    private String resolveTitle(PluginMenuContribution contribution) {
        if (contribution.titleBundleName() == null || contribution.titleBundleName().isBlank()) {
            return I18nUtil.getMessage(contribution.titleKey());
        }
        return I18nUtil.getMessage(contribution.titleBundleName(), contribution.titleClassLoader(), contribution.titleKey());
    }

    private void runPluginMenuAction(Component parent, PluginMenuContribution contribution) {
        try {
            Window parentWindow = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
            contribution.perform(new PluginMenuActionContext(parentWindow));
        } catch (Exception e) {
            log.error("Failed to run plugin menu contribution: {}", contribution.id(), e);
            if (parent == null) {
                return;
            }
            JOptionPane.showMessageDialog(parent,
                    e.getMessage(),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE);
        }
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

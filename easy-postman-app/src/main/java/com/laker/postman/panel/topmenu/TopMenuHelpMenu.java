package com.laker.postman.panel.topmenu;

import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.panel.topmenu.help.ChangelogDialog;
import com.laker.postman.panel.topmenu.help.MemoryTuningDialog;
import com.laker.postman.service.UpdateService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;

/**
 * 顶部帮助菜单。
 */
@Slf4j
@UtilityClass
class TopMenuHelpMenu {

    JMenu create(Component parent) {
        JMenu helpMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_HELP));

        JMenuItem updateMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_UPDATE));
        updateMenuItem.addActionListener(e -> checkUpdate());
        helpMenu.add(updateMenuItem);

        JMenuItem changelogMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_CHANGELOG));
        changelogMenuItem.addActionListener(e -> showChangelogDialog(parent));
        helpMenu.add(changelogMenuItem);

        JMenuItem memoryTuningMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_MEMORY_TUNING));
        memoryTuningMenuItem.addActionListener(e -> showMemoryTuningDialog(parent));
        helpMenu.add(memoryTuningMenuItem);

        JMenuItem feedbackMenuItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.MENU_HELP_FEEDBACK));
        feedbackMenuItem.addActionListener(e -> showFeedbackDialog(parent));
        helpMenu.add(feedbackMenuItem);

        return helpMenu;
    }

    private void checkUpdate() {
        BeanFactory.getBean(UpdateService.class).checkUpdateManually();
    }

    private void showChangelogDialog(Component parent) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window instanceof Frame frame) {
            ChangelogDialog.showDialog(frame);
        } else {
            log.warn("Cannot show changelog dialog: parent is not a Frame");
        }
    }

    private void showMemoryTuningDialog(Component parent) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        MemoryTuningDialog.showDialog(window instanceof Frame frame ? frame : null);
    }

    private void showFeedbackDialog(Component parent) {
        JOptionPane.showMessageDialog(TopMenuDialogOwner.resolve(parent),
                I18nUtil.getMessage(MessageKeys.FEEDBACK_MESSAGE),
                I18nUtil.getMessage(MessageKeys.FEEDBACK_TITLE),
                JOptionPane.INFORMATION_MESSAGE);
    }
}

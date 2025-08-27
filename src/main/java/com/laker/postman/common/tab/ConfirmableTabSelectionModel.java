package com.laker.postman.common.tab;

import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 支持切换前拦截的 selectionModel
 */
public class ConfirmableTabSelectionModel extends DefaultSingleSelectionModel {
    private final SidebarTabPanel sidebarTabPanel;
    private boolean ignore = false; // 防止递归

    public ConfirmableTabSelectionModel(SidebarTabPanel panel) {
        this.sidebarTabPanel = panel;
    }

    @Override
    public void setSelectedIndex(int index) {
        if (ignore) {
            super.setSelectedIndex(index);
            return;
        }
        int prevIndex = getSelectedIndex();
        // 只拦截常规tab切换
        if (prevIndex >= 0 && prevIndex < sidebarTabPanel.getTabInfos().size() && index != prevIndex && index < sidebarTabPanel.getTabInfos().size()) {
            Component prevComp = sidebarTabPanel.getTabbedPane().getComponentAt(prevIndex);
            if (prevComp instanceof EnvironmentPanel envPanel) {
                if (envPanel.isVariablesChanged()) {
                    int option = JOptionPane.showConfirmDialog(sidebarTabPanel,
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_SAVE_CHANGES),
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_SAVE_CHANGES_TITLE),
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.CANCEL_OPTION) {
                        // 阻止切换
                        return;
                    } else if (option == JOptionPane.YES_OPTION) {
                        envPanel.saveVariables();
                    } else if (option == JOptionPane.NO_OPTION) {
                        // 放弃更改，无需操作
                    }
                }
            }
        }
        // 允许切换
        ignore = true;
        super.setSelectedIndex(index);
        ignore = false;
    }
}
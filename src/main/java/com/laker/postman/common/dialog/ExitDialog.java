package com.laker.postman.common.dialog;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 退出确认对话框。
 * 用于在用户尝试关闭应用时显示确认对话框。
 * 如果用户选择“是”，则关闭应用程序。
 */
@Slf4j
public class ExitDialog {
    private ExitDialog() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 显示退出确认对话框，处理未保存内容。
     */
    public static void show() {
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        List<Integer> unsavedTabs = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) instanceof RequestEditSubPanel subPanel && subPanel.isModified()) {
                unsavedTabs.add(i);
            }
        }

        // 如果有未保存内容，弹出自定义对话框
        if (!unsavedTabs.isEmpty()) {
            String[] options = {
                    I18nUtil.getMessage(MessageKeys.EXIT_SAVE_ALL), // "全部保存"
                    I18nUtil.getMessage(MessageKeys.EXIT_DISCARD_ALL), // "全部不保存"
                    I18nUtil.getMessage(MessageKeys.EXIT_CANCEL) // "取消"
            };
            int result = JOptionPane.showOptionDialog(tabbedPane,
                    I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES),
                    I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES_TITLE),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (result == 2 || result == JOptionPane.CLOSED_OPTION) {
                // 用户取消，直接返回
                return;
            }
            if (result == 0) {
                // 全部保存
                for (Integer i : unsavedTabs) {
                    tabbedPane.setSelectedIndex(i);
                    editPanel.saveCurrentRequest();
                }
            }
            // result == 1 全部不保存，直接退出
        }
        // 没有未保存内容，或已处理完未保存内容，直接退出
        log.info("User chose to exit application");
        SingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}
package com.laker.postman.common.dialog;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
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

    public static void show() {
        // Check for unsaved changes
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        boolean hasUnsaved = false;
        // Collect all unsaved tab indexes
        List<Integer> unsavedTabs = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) instanceof RequestEditSubPanel subPanel) {
                if (subPanel.isModified()) {
                    hasUnsaved = true;
                    unsavedTabs.add(i);
                }
            }
        }
        if (hasUnsaved) { // If there are unsaved changes, prompt user to save
            int result = JOptionPane.showConfirmDialog(tabbedPane,
                    "There are unsaved changes. Save all?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {  // User chose cancel, exit dialog
                return;
            }
            if (result == JOptionPane.YES_OPTION) {  // User chose yes, save all unsaved tabs
                // Save all unsaved tabs
                for (Integer i : unsavedTabs) {
                    tabbedPane.setSelectedIndex(i);
                    editPanel.saveCurrentRequest();
                }
            }
            // Use invokeLater to ensure dialog order
            SwingUtilities.invokeLater(() -> {
                int exitResult = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION);
                if (exitResult != JOptionPane.YES_OPTION) {
                    return;
                }
                log.info("User chose to exit application");
                SingletonFactory.getInstance(MainFrame.class).dispose();
                System.exit(0);
            });
            return;
        }
        // No unsaved content, show exit confirmation directly
        int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        log.info("User chose to exit application");
        SingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}
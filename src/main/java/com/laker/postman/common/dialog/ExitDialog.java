package com.laker.postman.common.dialog;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.panel.collections.edit.RequestEditPanel;
import com.laker.postman.panel.collections.edit.RequestEditSubPanel;
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
        // 检查是否有未保存的更改
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        boolean hasUnsaved = false;
        // 收集所有未保存的tab索引
        List<Integer> unsavedTabs = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) instanceof RequestEditSubPanel subPanel) {
                if (subPanel.isModified()) {
                    hasUnsaved = true;
                    unsavedTabs.add(i);
                }
            }
        }
        if (hasUnsaved) { // 如果有未保存的更改，提示用户是否保存
            int result = JOptionPane.showConfirmDialog(tabbedPane,
                    "有未保存的更改，是否全部保存？",
                    "未保存的更改",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {  // 用户选择取消，退出对话框
                return;
            }
            if (result == JOptionPane.YES_OPTION) {  // 用户选择是，依次保存所有未保存的tab
                // 依次保存所有未保存的tab
                for (Integer i : unsavedTabs) {
                    tabbedPane.setSelectedIndex(i);
                    editPanel.saveCurrentRequest();
                }
            }
            // 这里用 invokeLater 保证弹窗顺序
            SwingUtilities.invokeLater(() -> {
                int exitResult = JOptionPane.showConfirmDialog(null, "确定要退出吗？", "退出", JOptionPane.YES_NO_OPTION);
                if (exitResult != JOptionPane.YES_OPTION) {
                    return;
                }
                log.info("用户选择退出应用程序");
                SingletonFactory.getInstance(MainFrame.class).dispose();
                System.exit(0);
            });
            return;
        }
        // 没有未保存内容，直接弹退出确认
        int result = JOptionPane.showConfirmDialog(null, "确定要退出吗？", "退出", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        log.info("用户选择退出应用程序");
        SingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}

package com.laker.postman.common.dialog;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

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
        // show confirm dialog
        int result = JOptionPane.showConfirmDialog(null, "确定要退出吗？", "退出", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        log.info("用户选择退出应用程序");
        SingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}

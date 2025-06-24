package com.laker.postman.common.dialog;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;

import javax.swing.*;

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
        SingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}

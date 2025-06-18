package com.laker.postman.common.dialog;

import javax.swing.*;
import java.awt.*;

public class LargeInputDialog {
    /**
     * 显示多行大输入框对话框
     * @param parent 父组件
     * @param title 标题
     * @param message 提示信息
     * @param rows 行数
     * @param columns 列数
     * @return 用户输入内容，取消返回null
     */
    public static String show(Component parent, String title, String message, int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        JScrollPane scrollPane = new JScrollPane(textArea);
        int result = JOptionPane.showConfirmDialog(
                parent,
                new Object[]{message, scrollPane},
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            return textArea.getText();
        }
        return null;
    }

    // 常用重载
    public static String show(Component parent, String title, String message) {
        return show(parent, title, message, 10, 60);
    }
}
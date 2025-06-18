package com.laker.postman.common.dialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AutoCloseDialog {

    /**
     * 显示一个定时自动关闭的对话框，可以在用户点击“确定”时提前关闭；
     *
     * @param title         对话框标题
     * @param message       显示消息内容
     * @param messageType   消息类型（如 JOptionPane.INFORMATION_MESSAGE）
     * @param timeoutMillis 自动关闭时间（毫秒）
     */
    public static void showAutoCloseDialog(String title, String message, int messageType, int timeoutMillis) {
        final boolean[] isClosed = {false};

        // 创建 JOptionPane
        JOptionPane optionPane = new JOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        JDialog dialog = optionPane.createDialog(title);

        // 设置居中
        dialog.setLocationRelativeTo(null);

        // 启动定时器，超时后关闭窗口
        Timer timer = new Timer(timeoutMillis, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isClosed[0]) {
                    dialog.dispose();
                    isClosed[0] = true;
                }
            }
        });
        timer.setRepeats(false); // 只执行一次
        timer.start();

        // 添加监听器，当用户手动点击按钮时取消定时器
        optionPane.addPropertyChangeListener(e -> {
            String prop = e.getPropertyName();
            if (prop.equals(JOptionPane.VALUE_PROPERTY)) {
                if (!isClosed[0]) {
                    timer.stop();
                    dialog.dispose();
                    isClosed[0] = true;
                }
            }
        });

        // 显示对话框
        dialog.setVisible(true);
    }

}
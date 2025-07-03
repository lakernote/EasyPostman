package com.laker.postman.common.dialog;

import javax.swing.*;
import java.awt.*;

public class LargeInputDialog {

    public static String show(Component parent, String title, String message, String defaultText) {
        JTextArea textArea = new JTextArea(10, 60);
        if (defaultText != null) {
            textArea.setText(defaultText);
            textArea.setCaretPosition(0);
        }
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
}
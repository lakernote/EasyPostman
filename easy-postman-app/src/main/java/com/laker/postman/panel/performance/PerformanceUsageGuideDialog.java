package com.laker.postman.panel.performance;

import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Window;

final class PerformanceUsageGuideDialog {
    private PerformanceUsageGuideDialog() {
    }

    static void show(Component parent) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
                owner,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_USAGE_HELP_TITLE),
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(createContent());
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JComponent createContent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JTextArea textArea = new JTextArea(I18nUtil.getMessage(MessageKeys.PERFORMANCE_USAGE_HELP_CONTENT));
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        textArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        textArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
}

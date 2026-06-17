package com.laker.postman.common.component.dialog;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.NotificationUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared single-line text input dialog for compact rename/create flows.
 */
public final class TextInputDialog {
    private static final int FIELD_WIDTH = 360;
    private static final int FIELD_HEIGHT = 32;

    private TextInputDialog() {
    }

    public static Optional<String> showRequired(Component parent,
                                                String title,
                                                String label,
                                                String initialValue,
                                                String emptyWarningMessage) {
        JDialog dialog = createDialog(parent, title);
        AtomicReference<String> result = new AtomicReference<>();

        JPanel content = createContentPanel();
        JLabel nameLabel = createLabel(label);
        JTextField nameField = createNameField(initialValue);
        content.add(nameLabel, BorderLayout.NORTH);
        content.add(nameField, BorderLayout.CENTER);

        JButton cancelButton = ModernButtonFactory.createButton(
                CommonI18n.get(CommonMessageKeys.BUTTON_CANCEL),
                false
        );
        JButton okButton = ModernButtonFactory.createButton(
                CommonI18n.get(CommonMessageKeys.GENERAL_OK),
                true
        );

        Runnable confirmAction = () -> {
            String value = nameField.getText() == null ? "" : nameField.getText().trim();
            if (value.isEmpty()) {
                if (emptyWarningMessage != null && !emptyWarningMessage.isBlank()) {
                    NotificationUtil.showWarning(emptyWarningMessage);
                }
                nameField.requestFocusInWindow();
                return;
            }
            result.set(value);
            dialog.dispose();
        };

        cancelButton.addActionListener(e -> dialog.dispose());
        okButton.addActionListener(e -> confirmAction.run());
        nameField.addActionListener(e -> confirmAction.run());

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(createFooter(cancelButton, okButton), BorderLayout.SOUTH);
        installDialogKeys(dialog, okButton);

        dialog.pack();
        dialog.setMinimumSize(dialog.getPreferredSize());
        dialog.setLocationRelativeTo(parent);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return Optional.ofNullable(result.get());
    }

    public static Optional<String> showRequiredName(Component parent,
                                                    String title,
                                                    String initialValue,
                                                    String emptyWarningMessage) {
        return showRequired(
                parent,
                title,
                CommonI18n.get(CommonMessageKeys.LABEL_NAME),
                initialValue,
                emptyWarningMessage
        );
    }

    private static JDialog createDialog(Component parent, String title) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        return dialog;
    }

    private static JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        return panel;
    }

    private static JLabel createLabel(String labelText) {
        JLabel label = new JLabel(labelText == null ? "" : labelText);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        return label;
    }

    private static JTextField createNameField(String initialValue) {
        JTextField nameField = new JTextField(25);
        nameField.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        if (initialValue != null && !initialValue.trim().isEmpty()) {
            nameField.setText(initialValue);
            nameField.selectAll();
        }
        SettingsInputStyle.apply(nameField);
        return nameField;
    }

    private static JPanel createFooter(JButton cancelButton, JButton okButton) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(footer);
        footer.add(cancelButton);
        footer.add(okButton);
        return footer;
    }

    private static void installDialogKeys(JDialog dialog, JButton okButton) {
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
}

package com.laker.postman.common.component.dialog;

import com.laker.postman.common.component.SyntaxEditorScrollPane;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;

public class CurlImportDialog extends JDialog {
    private static final Dimension DIALOG_SIZE = new Dimension(900, 600);
    private static final Dimension ACTION_BUTTON_SIZE = new Dimension(80, 30);
    private static final Insets CONTENT_PADDING = new Insets(5, 10, 10, 10);

    private RSyntaxTextArea curlArea;
    private boolean confirmed = false;

    private CurlImportDialog(Component parent, String title, String message, String defaultText) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);
        initComponents(message, defaultText);
        setLocationRelativeTo(parent);
    }

    private void initComponents(String message, String defaultText) {
        setLayout(new BorderLayout(10, 10));
        addMessageLabel(message);
        add(createEditorPanel(defaultText), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        configureDialogShortcuts();

        setPreferredSize(DIALOG_SIZE);
        pack();
    }

    private void addMessageLabel(String message) {
        if (message != null && !message.isEmpty()) {
            JLabel messageLabel = new JLabel(message);
            messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
            add(messageLabel, BorderLayout.NORTH);
        }
    }

    private JComponent createEditorPanel(String defaultText) {
        curlArea = createCurlEditor();
        if (defaultText != null && !defaultText.isEmpty()) {
            curlArea.setText(defaultText);
            curlArea.setCaretPosition(0);
        }

        SyntaxEditorScrollPane scrollPane = new SyntaxEditorScrollPane(curlArea);
        scrollPane.setLineNumbersEnabled(true);
        JPanel editorWrapper = new JPanel(new BorderLayout());
        editorWrapper.setBorder(BorderFactory.createEmptyBorder(
                CONTENT_PADDING.top,
                CONTENT_PADDING.left,
                CONTENT_PADDING.bottom,
                CONTENT_PADDING.right
        ));
        editorWrapper.add(scrollPane, BorderLayout.CENTER);
        return editorWrapper;
    }

    private RSyntaxTextArea createCurlEditor() {
        RSyntaxTextArea editor = new RSyntaxTextArea(20, 80);
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        editor.setCodeFoldingEnabled(false);
        editor.setEditable(true);
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        editor.setAntiAliasingEnabled(true);
        editor.setAutoIndentEnabled(true);
        editor.setTabSize(2);
        editor.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        EditorThemeUtil.loadTheme(editor);
        return editor;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));

        okButton.setPreferredSize(ACTION_BUTTON_SIZE);
        cancelButton.setPreferredSize(ACTION_BUTTON_SIZE);

        okButton.addActionListener(e -> confirmAndClose());
        cancelButton.addActionListener(e -> cancelAndClose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    private void configureDialogShortcuts() {
        getRootPane().registerKeyboardAction(
                e -> cancelAndClose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        getRootPane().registerKeyboardAction(
                e -> confirmAndClose(),
                KeyStroke.getKeyStroke("ctrl ENTER"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void confirmAndClose() {
        confirmed = true;
        dispose();
    }

    private void cancelAndClose() {
        confirmed = false;
        dispose();
    }

    public static String show(Component parent, String title, String message, String defaultText) {
        CurlImportDialog dialog = new CurlImportDialog(parent, title, message, defaultText);
        dialog.setVisible(true);

        if (dialog.confirmed) {
            return dialog.curlArea.getText();
        }
        return null;
    }
}

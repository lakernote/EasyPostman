package com.laker.postman.common.component.dialog;

import com.laker.postman.util.FontsUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class CurlImportDialog extends JDialog {
    private RSyntaxTextArea curlArea;
    private boolean confirmed = false;

    private CurlImportDialog(Component parent, String title, String message, String defaultText) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);
        initComponents(message, defaultText);
        setLocationRelativeTo(parent);
    }

    private void initComponents(String message, String defaultText) {
        setLayout(new BorderLayout(10, 10));

        // 消息标签
        if (message != null && !message.isEmpty()) {
            JLabel messageLabel = new JLabel(message);
            messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
            add(messageLabel, BorderLayout.NORTH);
        }

        // cURL 输入区域
        curlArea = new RSyntaxTextArea(20, 80);
        curlArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        curlArea.setCodeFoldingEnabled(false);
        curlArea.setEditable(true);
        curlArea.setLineWrap(true);
        curlArea.setWrapStyleWord(true);
        curlArea.setAntiAliasingEnabled(true);
        curlArea.setAutoIndentEnabled(true);
        curlArea.setTabSize(2);
        curlArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1)); // 比标准字体大1号

        // 尝试应用主题
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
            theme.apply(curlArea);
        } catch (IOException e) {
            // 忽略主题加载失败
        }

        if (defaultText != null && !defaultText.isEmpty()) {
            curlArea.setText(defaultText);
            curlArea.setCaretPosition(0);
        }

        RTextScrollPane scrollPane = new RTextScrollPane(curlArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setPreferredSize(new Dimension(80, 30));

        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 设置对话框大小
        setPreferredSize(new Dimension(900, 600));
        pack();

        // ESC 键关闭
        getRootPane().registerKeyboardAction(
                e -> {
                    confirmed = false;
                    dispose();
                },
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Enter 键确认（Ctrl+Enter）
        getRootPane().registerKeyboardAction(
                e -> {
                    confirmed = true;
                    dispose();
                },
                KeyStroke.getKeyStroke("ctrl ENTER"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
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


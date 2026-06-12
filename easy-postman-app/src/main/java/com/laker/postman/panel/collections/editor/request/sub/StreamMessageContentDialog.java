package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.ViewportClippedTokenPainter;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.function.Supplier;

final class StreamMessageContentDialog {

    private static final int MIN_WIDTH = 520;
    private static final int MIN_HEIGHT = 260;
    private static final int MAX_WIDTH = 900;
    private static final int MAX_HEIGHT = 620;
    private static final double VIEWPORT_RATIO = 0.72;
    private static final int CHARS_PER_WRAPPED_LINE = 96;
    private static final int DEFAULT_AVAILABLE_WIDTH = 1200;
    private static final int DEFAULT_AVAILABLE_HEIGHT = 800;

    private StreamMessageContentDialog() {
    }

    static void show(Component owner, String title, String rawContent, boolean formatAvailable,
                     Supplier<String> formattedContentSupplier) {
        String safeRawContent = safeText(rawContent);
        Window window = SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(window, title, Dialog.ModalityType.MODELESS);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        RSyntaxTextArea textArea = createContentEditor();
        setEditorText(textArea, safeRawContent);
        SearchableTextArea searchableTextArea = new SearchableTextArea(textArea, false);
        ToolWindowSurfaceStyle.applyDialogSurface(searchableTextArea);

        JPanel contentPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        contentPanel.add(searchableTextArea, BorderLayout.CENTER);

        JButton copyButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY), false);
        JButton formatButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_FORMAT), false);
        JButton rawButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_RAW), false);
        JButton closeButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE), false);
        setCompactButtonSize(copyButton);
        setCompactButtonSize(formatButton);
        setCompactButtonSize(rawButton);
        setCompactButtonSize(closeButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);
        buttonPanel.add(copyButton);
        buttonPanel.add(formatButton);
        buttonPanel.add(rawButton);
        buttonPanel.add(closeButton);

        JPanel rootPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(rootPanel);
        rootPanel.add(contentPanel, BorderLayout.CENTER);
        rootPanel.add(buttonPanel, BorderLayout.SOUTH);

        String[] formattedContent = new String[1];
        formatButton.setEnabled(formatAvailable && formattedContentSupplier != null);
        rawButton.setEnabled(false);

        copyButton.addActionListener(e -> Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(textArea.getText()), null));
        formatButton.addActionListener(e -> {
            if (formattedContent[0] == null) {
                formattedContent[0] = safeText(formattedContentSupplier.get());
            }
            setEditorText(textArea, formattedContent[0]);
            formatButton.setEnabled(false);
            rawButton.setEnabled(true);
        });
        rawButton.addActionListener(e -> {
            setEditorText(textArea, safeRawContent);
            formatButton.setEnabled(formatAvailable && formattedContentSupplier != null);
            rawButton.setEnabled(false);
        });
        closeButton.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(closeButton);

        dialog.setContentPane(rootPanel);
        dialog.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        dialog.setSize(preferredDialogSize(safeRawContent, resolveAvailableSize(owner)));
        dialog.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(closeButton::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private static RSyntaxTextArea createContentEditor() {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setEditable(false);
        textArea.setCodeFoldingEnabled(true);
        textArea.setLineWrap(false);
        textArea.setHighlightCurrentLine(false);
        textArea.setShowMatchedBracketPopup(false);
        textArea.setTokenPainterFactory(ignored -> new ViewportClippedTokenPainter());
        EditorThemeUtil.loadTheme(textArea);
        textArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        return textArea;
    }

    private static void setEditorText(RSyntaxTextArea textArea, String text) {
        textArea.setSyntaxEditingStyle(detectSyntax(text));
        textArea.setText(safeText(text));
        textArea.setCaretPosition(0);
    }

    private static String detectSyntax(String text) {
        if (JsonUtil.isTypeJSON(text)) {
            return SyntaxConstants.SYNTAX_STYLE_JSON;
        }
        String trimmed = safeText(text).trim();
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            String lower = trimmed.toLowerCase();
            if (lower.contains("<html")) {
                return SyntaxConstants.SYNTAX_STYLE_HTML;
            }
            if (lower.contains("<?xml")) {
                return SyntaxConstants.SYNTAX_STYLE_XML;
            }
        }
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    static Dimension preferredDialogSize(String content, Dimension availableSize) {
        Dimension safeAvailableSize = sanitizeAvailableSize(availableSize);
        TextMetrics metrics = measure(safeText(content));
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, metrics.maxLineLength() * 7 + 140));
        int height = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, metrics.visualLineCount() * 20 + 160));
        int widthCap = Math.max(MIN_WIDTH, (int) (safeAvailableSize.width * VIEWPORT_RATIO));
        int heightCap = Math.max(MIN_HEIGHT, (int) (safeAvailableSize.height * VIEWPORT_RATIO));
        return new Dimension(Math.min(width, widthCap), Math.min(height, heightCap));
    }

    private static Dimension resolveAvailableSize(Component owner) {
        Window window = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        if (window != null && window.getWidth() > 0 && window.getHeight() > 0) {
            return window.getSize();
        }
        try {
            return Toolkit.getDefaultToolkit().getScreenSize();
        } catch (HeadlessException ignored) {
            return new Dimension(DEFAULT_AVAILABLE_WIDTH, DEFAULT_AVAILABLE_HEIGHT);
        }
    }

    private static Dimension sanitizeAvailableSize(Dimension availableSize) {
        if (availableSize == null || availableSize.width <= 0 || availableSize.height <= 0) {
            return new Dimension(DEFAULT_AVAILABLE_WIDTH, DEFAULT_AVAILABLE_HEIGHT);
        }
        return availableSize;
    }

    private static TextMetrics measure(String content) {
        String[] lines = content.split("\\R", -1);
        int maxLineLength = 0;
        int visualLineCount = 0;
        for (String line : lines) {
            int lineLength = line.length();
            maxLineLength = Math.max(maxLineLength, Math.min(lineLength, CHARS_PER_WRAPPED_LINE));
            visualLineCount += Math.max(1, (lineLength + CHARS_PER_WRAPPED_LINE - 1) / CHARS_PER_WRAPPED_LINE);
        }
        return new TextMetrics(maxLineLength, Math.max(1, visualLineCount));
    }

    private static void setCompactButtonSize(AbstractButton button) {
        Dimension size = new Dimension(88, 30);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
    }

    private static String safeText(String text) {
        return text == null ? "" : text;
    }

    private record TextMetrics(int maxLineLength, int visualLineCount) {
    }
}

package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.constants.ModernColors;
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
import java.util.List;
import java.util.function.Supplier;

final class StreamMessageContentDialog {

    private static final int MIN_WIDTH = 520;
    private static final int MIN_HEIGHT = 420;
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
        show(owner, title, List.of(), rawContent, formatAvailable, formattedContentSupplier);
    }

    static void show(Component owner, String title, List<DetailField> detailFields, String rawContent,
                     boolean formatAvailable, Supplier<String> formattedContentSupplier) {
        String safeRawContent = safeText(rawContent);
        List<DetailField> safeDetailFields = detailFields == null ? List.of() : detailFields;
        Window window = SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(window, title, Dialog.ModalityType.MODELESS);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        RSyntaxTextArea textArea = createContentEditor();
        InitialContent initialContent = resolveInitialContent(safeRawContent, formatAvailable, formattedContentSupplier);
        setEditorText(textArea, initialContent.text());
        SearchableTextArea searchableTextArea = new SearchableTextArea(textArea, false);
        ToolWindowSurfaceStyle.applyDialogSurface(searchableTextArea);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 6));
        ToolWindowSurfaceStyle.applyDialogSurface(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        if (!safeDetailFields.isEmpty()) {
            JPanel metadataPanel = createMetadataPanel(safeDetailFields);
            contentPanel.add(metadataPanel, BorderLayout.NORTH);
        }
        contentPanel.add(searchableTextArea, BorderLayout.CENTER);

        JButton copyButton = createFooterButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_COPY),
                false,
                FooterAction.COPY
        );
        JButton formatButton = createFooterButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_FORMAT),
                false,
                FooterAction.FORMAT
        );
        JButton rawButton = createFooterButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_RAW),
                false,
                FooterAction.RAW
        );
        JButton closeButton = createFooterButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE),
                true,
                FooterAction.CLOSE
        );

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

        String[] formattedContent = new String[]{initialContent.showingFormatted() ? initialContent.text() : null};
        formatButton.setEnabled(formatAvailable && formattedContentSupplier != null && !initialContent.showingFormatted());
        rawButton.setEnabled(initialContent.showingFormatted());

        copyButton.addActionListener(e -> Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(buildDetailCopyText(safeDetailFields, textArea.getText())), null));
        formatButton.addActionListener(e -> {
            if (formattedContent[0] == null) {
                formattedContent[0] = resolveFormattedContent(safeRawContent, formattedContentSupplier);
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

    static JPanel createMetadataPanel(List<DetailField> detailFields) {
        JPanel panel = new JPanel(new GridLayout(0, 4, 12, 4));
        ToolWindowSurfaceStyle.applyDialogSection(panel);
        for (DetailField field : detailFields) {
            JLabel label = new JLabel(field.label());
            label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            label.setForeground(ModernColors.getTextSecondary());

            JLabel value = new JLabel(safeText(field.value()));
            value.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            value.setToolTipText(safeText(field.value()));
            panel.add(label);
            panel.add(value);
        }
        return panel;
    }

    static String buildDetailCopyText(List<DetailField> detailFields, String content) {
        StringBuilder sb = new StringBuilder();
        if (detailFields != null) {
            for (DetailField field : detailFields) {
                sb.append(field.label()).append(": ").append(safeText(field.value())).append('\n');
            }
        }
        if (!sb.isEmpty()) {
            sb.append('\n');
        }
        sb.append(safeText(content));
        return sb.toString();
    }

    static InitialContent resolveInitialContent(String rawContent, boolean formatAvailable,
                                                Supplier<String> formattedContentSupplier) {
        String safeRawContent = safeText(rawContent);
        if (!formatAvailable || formattedContentSupplier == null) {
            return new InitialContent(safeRawContent, false);
        }
        return new InitialContent(resolveFormattedContent(safeRawContent, formattedContentSupplier), true);
    }

    static JButton createFooterButton(String text, boolean primary, FooterAction action) {
        return ModernButtonFactory.createCompactButton(text, primary, action.iconPath());
    }

    private static RSyntaxTextArea createContentEditor() {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setEditable(false);
        textArea.setCodeFoldingEnabled(true);
        textArea.setLineWrap(false);
        textArea.setHighlightCurrentLine(false);
        textArea.setShowMatchedBracketPopup(false);
        EditorThemeUtil.loadTheme(textArea);
        EditorThemeUtil.installViewportClippedTokenPainter(textArea);
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

    private static String safeText(String text) {
        return text == null ? "" : text;
    }

    private static String resolveFormattedContent(String fallback, Supplier<String> formattedContentSupplier) {
        try {
            return safeText(formattedContentSupplier.get());
        } catch (RuntimeException ignored) {
            return safeText(fallback);
        }
    }

    private record TextMetrics(int maxLineLength, int visualLineCount) {
    }

    record DetailField(String label, String value) {
    }

    record InitialContent(String text, boolean showingFormatted) {
    }

    enum FooterAction {
        COPY("icons/copy.svg"),
        FORMAT("icons/format.svg"),
        RAW("icons/code.svg"),
        CLOSE("icons/close.svg");

        private final String iconPath;

        FooterAction(String iconPath) {
            this.iconPath = iconPath;
        }

        String iconPath() {
            return iconPath;
        }
    }
}

package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Request body 批量编辑支持。
 */
final class RequestBodyBulkEditSupport {
    private static final String FORM_DATA_FILE_PREFIX = "@";
    private static final String FORM_DATA_TEXT_ESCAPE_PREFIX = "@@";

    private final Component parent;
    private final FormDataTablePanel formDataTablePanel;
    private final FormUrlencodedTablePanel formUrlencodedTablePanel;

    RequestBodyBulkEditSupport(Component parent,
                               FormDataTablePanel formDataTablePanel,
                               FormUrlencodedTablePanel formUrlencodedTablePanel) {
        this.parent = parent;
        this.formDataTablePanel = formDataTablePanel;
        this.formUrlencodedTablePanel = formUrlencodedTablePanel;
    }

    void showBulkEditDialog(String bodyType) {
        if (RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(bodyType)) {
            showBulkEditDialog(
                    RequestBodyPanel.BODY_TYPE_FORM_DATA,
                    buildFormDataBulkText(),
                    "File: key: @/path/to/file; Text starting with @: key: @@literal",
                    this::parseFormDataBulkText
            );
            return;
        }
        if (RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(bodyType)) {
            showBulkEditDialog(
                    RequestBodyPanel.BODY_TYPE_FORM_URLENCODED,
                    buildFormUrlencodedBulkText(),
                    null,
                    this::parseFormUrlencodedBulkText
            );
        }
    }

    private void showBulkEditDialog(String title, String initialText, String extraHint, Consumer<String> onConfirm) {
        JTextArea textArea = new JTextArea(initialText);
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        textArea.setBackground(ModernColors.getInputBackgroundColor());
        textArea.setForeground(ModernColors.getTextPrimary());
        textArea.setCaretColor(ModernColors.PRIMARY);
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6));
        hintPanel.add(formatLabel);

        if (extraHint != null && !extraHint.isBlank()) {
            JLabel extraHintLabel = new JLabel(extraHint);
            extraHintLabel.setForeground(ModernColors.getTextSecondary());
            extraHintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
            extraHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            hintPanel.add(Box.createVerticalStrut(4));
            hintPanel.add(extraHintLabel);
        }
        hintPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.add(hintPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(window, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(650, 400);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(parent);

        okButton.addActionListener(e -> {
            onConfirm.accept(textArea.getText());
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.setVisible(true);
    }

    private String buildFormDataBulkText() {
        StringBuilder text = new StringBuilder();
        for (HttpFormData param : formDataTablePanel.getFormDataList()) {
            if (param.getKey().isEmpty()) {
                continue;
            }
            String renderedValue = param.isFile()
                    ? FORM_DATA_FILE_PREFIX + param.getValue()
                    : escapeFormDataTextValue(param.getValue());
            text.append(param.getKey()).append(": ").append(renderedValue).append("\n");
        }
        return text.toString();
    }

    private String buildFormUrlencodedBulkText() {
        StringBuilder text = new StringBuilder();
        for (HttpFormUrlencoded param : formUrlencodedTablePanel.getFormDataList()) {
            if (!param.getKey().isEmpty()) {
                text.append(param.getKey()).append(": ").append(param.getValue()).append("\n");
            }
        }
        return text.toString();
    }

    private void parseFormDataBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            formDataTablePanel.setFormDataList(new ArrayList<>());
            return;
        }

        List<FormDataRowState> rowStates = new ArrayList<>();
        for (HttpFormData existing : formDataTablePanel.getFormDataList()) {
            rowStates.add(new FormDataRowState(
                    existing.getKey().trim().toLowerCase(Locale.ROOT),
                    existing.isEnabled(),
                    HttpFormData.normalizeType(existing.getType())
            ));
        }

        List<HttpFormData> params = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            ParsedKeyValue parsed = parseKeyValueLine(line);
            FormDataRowState matchedState = takeNextFormDataRowState(rowStates, parsed.key);
            ParsedFormDataLine parsedFormData = parseFormDataLine(parsed.key, parsed.value, matchedState);
            if (!parsedFormData.key.isEmpty()) {
                boolean enabled = matchedState != null ? matchedState.isEnabled() : true;
                params.add(new HttpFormData(enabled, parsedFormData.key, parsedFormData.type, parsedFormData.value));
            }
        }

        formDataTablePanel.setFormDataList(params);
    }

    private void parseFormUrlencodedBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            formUrlencodedTablePanel.setFormDataList(new ArrayList<>());
            return;
        }

        List<RowEnabledState> rowStates = new ArrayList<>();
        for (HttpFormUrlencoded existing : formUrlencodedTablePanel.getFormDataList()) {
            rowStates.add(new RowEnabledState(
                    existing.getKey().trim().toLowerCase(Locale.ROOT),
                    existing.isEnabled()
            ));
        }

        List<HttpFormUrlencoded> params = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            ParsedKeyValue parsed = parseKeyValueLine(line);
            if (!parsed.key.isEmpty()) {
                RowEnabledState matchedState = takeNextRowEnabledState(rowStates, parsed.key);
                boolean enabled = matchedState != null ? matchedState.isEnabled() : true;
                params.add(new HttpFormUrlencoded(enabled, parsed.key, parsed.value));
            }
        }

        formUrlencodedTablePanel.setFormDataList(params);
    }

    private ParsedFormDataLine parseFormDataLine(String rawKey, String rawValue, FormDataRowState matchedState) {
        String key = normalizeFormDataKey(rawKey);
        if (key.isEmpty()) {
            return new ParsedFormDataLine("", HttpFormData.TYPE_TEXT, "");
        }

        String explicitType = resolveExplicitFormDataType(rawValue);
        String type = explicitType != null
                ? explicitType
                : matchedState != null ? matchedState.type : HttpFormData.TYPE_TEXT;
        String value = normalizeFormDataValue(rawValue, type);
        return new ParsedFormDataLine(key, type, value);
    }

    private ParsedKeyValue parseKeyValueLine(String line) {
        int colonIdx = line.indexOf(':');
        int equalsIdx = line.indexOf('=');
        int separatorIdx = resolveSeparatorIndex(colonIdx, equalsIdx);
        if (separatorIdx > 0) {
            return new ParsedKeyValue(
                    line.substring(0, separatorIdx).trim(),
                    line.substring(separatorIdx + 1).trim()
            );
        }
        return new ParsedKeyValue(line.trim(), "");
    }

    private int resolveSeparatorIndex(int colonIdx, int equalsIdx) {
        if (colonIdx > 0 && equalsIdx > 0) {
            return Math.min(colonIdx, equalsIdx);
        }
        if (colonIdx > 0) {
            return colonIdx;
        }
        return equalsIdx > 0 ? equalsIdx : -1;
    }

    private String resolveExplicitFormDataType(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        if (rawValue.startsWith(FORM_DATA_TEXT_ESCAPE_PREFIX)) {
            return HttpFormData.TYPE_TEXT;
        }
        if (rawValue.startsWith(FORM_DATA_FILE_PREFIX)) {
            return HttpFormData.TYPE_FILE;
        }
        return null;
    }

    private String normalizeFormDataKey(String rawKey) {
        return rawKey == null ? "" : rawKey.trim();
    }

    private String normalizeFormDataValue(String rawValue, String type) {
        if (rawValue == null) {
            return "";
        }
        if (HttpFormData.TYPE_FILE.equals(type)) {
            return rawValue.startsWith(FORM_DATA_FILE_PREFIX) ? rawValue.substring(1) : rawValue;
        }
        return unescapeFormDataTextValue(rawValue);
    }

    private String escapeFormDataTextValue(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith(FORM_DATA_FILE_PREFIX) ? FORM_DATA_FILE_PREFIX + value : value;
    }

    private String unescapeFormDataTextValue(String value) {
        if (value.startsWith(FORM_DATA_TEXT_ESCAPE_PREFIX)) {
            return value.substring(1);
        }
        return value;
    }

    private FormDataRowState takeNextFormDataRowState(List<FormDataRowState> rowStates, String key) {
        String normalizedKey = normalizeFormDataKey(key).toLowerCase(Locale.ROOT);
        for (FormDataRowState rowState : rowStates) {
            if (rowState.tryMatch(normalizedKey)) {
                return rowState;
            }
        }
        return null;
    }

    private RowEnabledState takeNextRowEnabledState(List<RowEnabledState> rowStates, String key) {
        String normalizedKey = normalizeFormDataKey(key).toLowerCase(Locale.ROOT);
        for (RowEnabledState rowState : rowStates) {
            if (rowState.tryMatch(normalizedKey)) {
                return rowState;
            }
        }
        return null;
    }

    private static final class ParsedKeyValue {
        private final String key;
        private final String value;

        private ParsedKeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class ParsedFormDataLine {
        private final String key;
        private final String type;
        private final String value;

        private ParsedFormDataLine(String key, String type, String value) {
            this.key = key;
            this.type = type;
            this.value = value;
        }
    }

    private static class RowEnabledState {
        private final String normalizedKey;
        private final boolean enabled;
        private boolean consumed;

        private RowEnabledState(String normalizedKey, boolean enabled) {
            this.normalizedKey = normalizedKey;
            this.enabled = enabled;
        }

        protected final boolean isEnabled() {
            return enabled;
        }

        protected final boolean tryMatch(String normalizedKey) {
            if (consumed || !this.normalizedKey.equals(normalizedKey)) {
                return false;
            }
            consumed = true;
            return true;
        }
    }

    private static final class FormDataRowState extends RowEnabledState {
        private final String type;

        private FormDataRowState(String normalizedKey, boolean enabled, String type) {
            super(normalizedKey, enabled);
            this.type = type;
        }
    }
}

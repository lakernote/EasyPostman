package com.laker.postman.plugin.redis;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.swing.*;
import java.awt.*;

final class SearchTextField extends JTextField {

    SearchTextField() {
        setPreferredSize(new Dimension(220, 28));
        setMinimumSize(new Dimension(50, 28));
    }

    void setNoResult(boolean noResult) {
        putClientProperty(FlatClientProperties.OUTLINE,
                noResult ? FlatClientProperties.OUTLINE_ERROR : null);
    }
}

final class SearchableTextArea extends JPanel {

    SearchableTextArea(RSyntaxTextArea textArea) {
        this(textArea, true);
    }

    SearchableTextArea(RSyntaxTextArea textArea, boolean enableLineNumbers) {
        setLayout(new BorderLayout());
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(enableLineNumbers);
        add(scrollPane, BorderLayout.CENTER);
    }
}

class PrimaryButton extends JButton {

    private static final int ICON_SIZE = 14;

    PrimaryButton(String text, String iconPath) {
        super(text);
        initButton(iconPath, Color.WHITE);
        setForeground(Color.WHITE);
        putClientProperty("JButton.buttonType", "roundRect");
        putClientProperty(FlatClientProperties.STYLE, "background:#1677ff; borderWidth:0");
    }

    private void initButton(String iconPath, Color iconColor) {
        if (iconPath != null && !iconPath.isBlank()) {
            FlatSVGIcon icon = new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE);
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> iconColor));
            setIcon(icon);
            setIconTextGap(4);
        }
        setFont(FontsUtil.getDefaultFont(Font.BOLD));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}

class SecondaryButton extends JButton {

    private static final int ICON_SIZE = 14;

    SecondaryButton(String text, String iconPath) {
        super(text);
        if (iconPath != null && !iconPath.isBlank()) {
            FlatSVGIcon icon = new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE);
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Label.foreground")));
            setIcon(icon);
            setIconTextGap(4);
        }
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty("JButton.buttonType", "roundRect");
    }
}

final class RefreshButton extends JButton {

    RefreshButton() {
        setIcon(new FlatSVGIcon("icons/refresh.svg", 18, 18));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}

final class ClearButton extends JButton {

    ClearButton() {
        setIcon(new FlatSVGIcon("icons/clear.svg", 18, 18));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}

final class FormatButton extends JButton {

    FormatButton() {
        setIcon(new FlatSVGIcon("icons/format.svg", 16, 16));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}

final class EditorThemeUtil {

    private EditorThemeUtil() {
    }

    static void loadTheme(RSyntaxTextArea area) {
        area.setBackground(UIManager.getColor("TextArea.background"));
        area.setCurrentLineHighlightColor(UIManager.getColor("Component.focusedBorderColor"));
        area.setCaretColor(UIManager.getColor("TextArea.caretForeground"));
        area.setSelectionColor(UIManager.getColor("TextArea.selectionBackground"));
    }
}

final class FontsUtil {

    private FontsUtil() {
    }

    static Font getDefaultFont(int style) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null) {
            base = UIManager.getFont("TextArea.font");
        }
        if (base == null) {
            base = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        }
        return base.deriveFont(style);
    }
}

final class NotificationUtil {

    private NotificationUtil() {
    }

    static void showSuccess(String message) {
        JOptionPane.showMessageDialog(null, message, "Redis Plugin", JOptionPane.INFORMATION_MESSAGE);
    }

    static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Redis Plugin", JOptionPane.ERROR_MESSAGE);
    }
}

final class JsonUtil {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
            .build();

    private JsonUtil() {
    }

    static boolean isTypeJSON(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return node != null && (node.isObject() || node.isArray());
        } catch (Exception ignore) {
            return false;
        }
    }

    static String toJsonPrettyStr(String json) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(json));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid json", e);
        }
    }

    static String toJsonPrettyStr(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot render json", e);
        }
    }
}

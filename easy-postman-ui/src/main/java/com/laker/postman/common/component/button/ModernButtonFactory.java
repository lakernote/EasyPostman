package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Shared modern button styling used across dialogs and settings panels.
 */
public final class ModernButtonFactory {

    static final String PRIMARY_STYLE_CLASS = "easyPostmanPrimary";
    static final String SECONDARY_STYLE_CLASS = "easyPostmanSecondary";
    static final String TOGGLE_STYLE_CLASS = "easyPostmanToggle";
    public static final int COMPACT_BUTTON_HEIGHT = 30;
    static final int COMPACT_BUTTON_MIN_WIDTH = 72;
    private static final Dimension DEFAULT_SIZE = new Dimension(100, 34);
    private static final int DEFAULT_ICON_SIZE = 16;
    static final int COMPACT_ICON_SIZE = 14;
    private static final int COMPACT_HORIZONTAL_PADDING = 24;

    private ModernButtonFactory() {
    }

    public static JButton createButton(String text, boolean primary) {
        JButton button = new JButton(text);
        configureBaseButton(button, primary ? PRIMARY_STYLE_CLASS : SECONDARY_STYLE_CLASS);
        return button;
    }

    public static JButton createButton(String text, boolean primary, String iconPath) {
        return createButton(text, primary, iconPath, DEFAULT_ICON_SIZE);
    }

    public static JButton createButton(String text, boolean primary, String iconPath, int iconSize) {
        JButton button = createButton(text, primary);
        configureButtonIcon(button, primary, iconPath, iconSize);
        return button;
    }

    public static JButton createCompactButton(String text, boolean primary, String iconPath) {
        JButton button = createButton(text, primary);
        configureButtonIcon(button, primary, iconPath, COMPACT_ICON_SIZE);
        configureCompactButton(button);
        return button;
    }

    static void configureCompactButton(AbstractButton button) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        Dimension size = new Dimension(
                compactButtonWidth(textWidth(button), iconWidth(button), button.getIconTextGap()),
                COMPACT_BUTTON_HEIGHT
        );
        button.putClientProperty(FlatClientProperties.STYLE,
                "minimumWidth: 0; minimumHeight: " + COMPACT_BUTTON_HEIGHT
                        + "; margin: 2,8,2,8; arc: 6");
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    static int compactButtonWidth(int textWidth, int iconWidth, int iconTextGap) {
        return Math.max(COMPACT_BUTTON_MIN_WIDTH, textWidth + iconWidth + iconTextGap + COMPACT_HORIZONTAL_PADDING);
    }

    public static JToggleButton createToggleButton(String text) {
        JToggleButton button = new JToggleButton(text);
        configureBaseButton(button, TOGGLE_STYLE_CLASS);
        return button;
    }

    static void configureBaseButton(AbstractButton button, String styleClass) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        button.setPreferredSize(DEFAULT_SIZE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setRolloverEnabled(true);
        button.putClientProperty(FlatClientProperties.STYLE_CLASS, styleClass);
    }

    static void configureButtonIcon(AbstractButton button, boolean primary, String iconPath, int iconSize) {
        if (iconPath == null || iconPath.isBlank()) {
            return;
        }
        button.setIcon(primary
                ? IconUtil.createOnPrimary(iconPath, iconSize, iconSize)
                : IconUtil.createThemed(iconPath, iconSize, iconSize));
        button.setIconTextGap(6);
    }

    private static int textWidth(AbstractButton button) {
        String text = button.getText();
        return text == null || text.isBlank() ? 0 : button.getFontMetrics(button.getFont()).stringWidth(text);
    }

    private static int iconWidth(AbstractButton button) {
        Icon icon = button.getIcon();
        return icon == null ? 0 : icon.getIconWidth();
    }
}

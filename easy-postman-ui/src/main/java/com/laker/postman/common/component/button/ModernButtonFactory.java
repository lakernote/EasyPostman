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
    private static final Dimension DEFAULT_SIZE = new Dimension(100, 34);
    private static final int DEFAULT_ICON_SIZE = 16;

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

    public static JToggleButton createToggleButton(String text) {
        JToggleButton button = new JToggleButton(text);
        configureBaseButton(button, TOGGLE_STYLE_CLASS);
        return button;
    }

    private static void configureBaseButton(AbstractButton button, String styleClass) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        button.setPreferredSize(DEFAULT_SIZE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setRolloverEnabled(true);
        button.putClientProperty(FlatClientProperties.STYLE_CLASS, styleClass);
    }

    private static void configureButtonIcon(AbstractButton button, boolean primary, String iconPath, int iconSize) {
        if (iconPath == null || iconPath.isBlank()) {
            return;
        }
        button.setIcon(primary
                ? IconUtil.createOnPrimary(iconPath, iconSize, iconSize)
                : IconUtil.createThemed(iconPath, iconSize, iconSize));
        button.setIconTextGap(6);
    }
}

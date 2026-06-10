package com.laker.postman.common.constants;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * EasyPostman semantic color tokens backed by FlatLaf UIDefaults.
 * <p>
 * Keep reusable UI code reading colors from this class instead of branching on
 * light/dark mode. Theme properties define the real values.
 */
@UtilityClass
public class ThemeColors {

    public static final String PRIMARY = "EasyPostman.color.primary";
    public static final String PRIMARY_DARK = "EasyPostman.color.primary.dark";
    public static final String PRIMARY_DARKER = "EasyPostman.color.primary.darker";
    public static final String PRIMARY_LIGHT = "EasyPostman.color.primary.light";
    public static final String PRIMARY_LIGHTER = "EasyPostman.color.primary.lighter";
    public static final String SECONDARY = "EasyPostman.color.secondary";
    public static final String SECONDARY_DARK = "EasyPostman.color.secondary.dark";
    public static final String SECONDARY_LIGHT = "EasyPostman.color.secondary.light";
    public static final String SECONDARY_LIGHTER = "EasyPostman.color.secondary.lighter";
    public static final String ACCENT = "EasyPostman.color.accent";
    public static final String ACCENT_LIGHT = "EasyPostman.color.accent.light";
    public static final String SUCCESS = "EasyPostman.color.success";
    public static final String SUCCESS_DARK = "EasyPostman.color.success.dark";
    public static final String ERROR = "EasyPostman.color.error";
    public static final String ERROR_DARK = "EasyPostman.color.error.dark";
    public static final String ERROR_DARKER = "EasyPostman.color.error.darker";
    public static final String WARNING = "EasyPostman.color.warning";
    public static final String WARNING_DARK = "EasyPostman.color.warning.dark";
    public static final String WARNING_DARKER = "EasyPostman.color.warning.darker";
    public static final String INFO = "EasyPostman.color.info";
    public static final String NEUTRAL = "EasyPostman.color.neutral";
    public static final String NEUTRAL_DARK = "EasyPostman.color.neutral.dark";
    public static final String NEUTRAL_DARKER = "EasyPostman.color.neutral.darker";
    public static final String GIT_COMMIT = "EasyPostman.color.git.commit";
    public static final String GIT_PUSH = "EasyPostman.color.git.push";
    public static final String GIT_PULL = "EasyPostman.color.git.pull";
    public static final String VARIABLE_CONTEXT = "EasyPostman.color.variable.context";
    public static final String VARIABLE_ITERATION_DATA = "EasyPostman.color.variable.iterationData";
    public static final String VARIABLE_GROUP = "EasyPostman.color.variable.group";
    public static final String VARIABLE_ENVIRONMENT = "EasyPostman.color.variable.environment";
    public static final String VARIABLE_GLOBAL = "EasyPostman.color.variable.global";
    public static final String VARIABLE_BUILT_IN = "EasyPostman.color.variable.builtIn";

    public static final String TEXT_PRIMARY = "EasyPostman.text.primary";
    public static final String TEXT_SECONDARY = "EasyPostman.text.secondary";
    public static final String TEXT_HINT = "EasyPostman.text.hint";
    public static final String TEXT_DISABLED = "EasyPostman.text.disabled";
    public static final String TEXT_INVERSE = "EasyPostman.text.inverse";

    public static final String BACKGROUND = "EasyPostman.background";
    public static final String SURFACE = "EasyPostman.surface";
    public static final String WINDOW_CHROME_BACKGROUND = "EasyPostman.window.chrome.background";
    public static final String DIALOG_CHROME_BACKGROUND = "EasyPostman.dialog.chrome.background";
    public static final String INPUT_BACKGROUND = "EasyPostman.input.background";
    public static final String TAB_BACKGROUND = "EasyPostman.tab.background";
    public static final String TAB_SELECTED_BACKGROUND = "EasyPostman.tab.selected.background";
    public static final String TAB_HOVER_BACKGROUND = "EasyPostman.tab.hover.background";
    public static final String TAB_SEPARATOR = "EasyPostman.tab.separator";
    public static final String HOVER_BACKGROUND = "EasyPostman.hover.background";
    public static final String SELECTION_BACKGROUND = "EasyPostman.selection.background";
    public static final String BUTTON_PRESSED_BACKGROUND = "EasyPostman.button.pressed.background";
    public static final String BUTTON_DISABLED_BACKGROUND = "EasyPostman.button.disabled.background";
    public static final String BORDER_LIGHT = "EasyPostman.border.light";
    public static final String BORDER_MEDIUM = "EasyPostman.border.medium";
    public static final String DIVIDER = "EasyPostman.divider";
    public static final String EMPTY_CELL_BACKGROUND = "EasyPostman.emptyCell.background";
    public static final String SCROLLBAR_TRACK = "EasyPostman.scrollbar.track";
    public static final String SCROLLBAR_THUMB = "EasyPostman.scrollbar.thumb";
    public static final String SCROLLBAR_THUMB_HOVER = "EasyPostman.scrollbar.thumbHover";
    public static final String WARNING_BACKGROUND = "EasyPostman.warning.background";
    public static final String WARNING_BORDER = "EasyPostman.warning.border";
    public static final String SEARCH_HIGHLIGHT_BACKGROUND = "EasyPostman.search.highlight.background";
    public static final String SEARCH_CURRENT_HIGHLIGHT_BACKGROUND = "EasyPostman.search.currentHighlight.background";
    public static final String SPLASH_GRADIENT_START = "EasyPostman.splash.gradient.start";
    public static final String SPLASH_GRADIENT_END = "EasyPostman.splash.gradient.end";

    public static final String CONSOLE_TEXT_AREA_BACKGROUND = "EasyPostman.console.textArea.background";
    public static final String CONSOLE_TEXT = "EasyPostman.console.text";
    public static final String CONSOLE_INFO = "EasyPostman.console.info";
    public static final String CONSOLE_DEBUG = "EasyPostman.console.debug";
    public static final String CONSOLE_WARN = "EasyPostman.console.warn";
    public static final String CONSOLE_ERROR = "EasyPostman.console.error";
    public static final String CONSOLE_CLASS_NAME = "EasyPostman.console.className";
    public static final String CONSOLE_METHOD_NAME = "EasyPostman.console.methodName";
    public static final String CONSOLE_TOOLBAR_BACKGROUND = "EasyPostman.console.toolbar.background";
    public static final String CONSOLE_TOOLBAR_BORDER = "EasyPostman.console.toolbar.border";
    public static final String CONSOLE_SELECTION_BACKGROUND = "EasyPostman.console.selection.background";

    public static final String NOTIFICATION_BACKGROUND = "EasyPostman.notification.background";
    public static final String NOTIFICATION_BORDER = "EasyPostman.notification.border";
    public static final String NOTIFICATION_DIVIDER = "EasyPostman.notification.divider";
    public static final String NOTIFICATION_BODY_FOREGROUND = "EasyPostman.notification.bodyForeground";

    public static final List<String> REQUIRED_KEYS = List.of(
            PRIMARY,
            PRIMARY_DARK,
            PRIMARY_DARKER,
            PRIMARY_LIGHT,
            PRIMARY_LIGHTER,
            SECONDARY,
            SECONDARY_DARK,
            SECONDARY_LIGHT,
            SECONDARY_LIGHTER,
            ACCENT,
            ACCENT_LIGHT,
            SUCCESS,
            SUCCESS_DARK,
            ERROR,
            ERROR_DARK,
            ERROR_DARKER,
            WARNING,
            WARNING_DARK,
            WARNING_DARKER,
            INFO,
            NEUTRAL,
            NEUTRAL_DARK,
            NEUTRAL_DARKER,
            GIT_COMMIT,
            GIT_PUSH,
            GIT_PULL,
            VARIABLE_CONTEXT,
            VARIABLE_ITERATION_DATA,
            VARIABLE_GROUP,
            VARIABLE_ENVIRONMENT,
            VARIABLE_GLOBAL,
            VARIABLE_BUILT_IN,
            TEXT_PRIMARY,
            TEXT_SECONDARY,
            TEXT_HINT,
            TEXT_DISABLED,
            TEXT_INVERSE,
            BACKGROUND,
            SURFACE,
            WINDOW_CHROME_BACKGROUND,
            DIALOG_CHROME_BACKGROUND,
            INPUT_BACKGROUND,
            TAB_BACKGROUND,
            TAB_SELECTED_BACKGROUND,
            TAB_HOVER_BACKGROUND,
            TAB_SEPARATOR,
            HOVER_BACKGROUND,
            SELECTION_BACKGROUND,
            BUTTON_PRESSED_BACKGROUND,
            BUTTON_DISABLED_BACKGROUND,
            BORDER_LIGHT,
            BORDER_MEDIUM,
            DIVIDER,
            EMPTY_CELL_BACKGROUND,
            SCROLLBAR_TRACK,
            SCROLLBAR_THUMB,
            SCROLLBAR_THUMB_HOVER,
            WARNING_BACKGROUND,
            WARNING_BORDER,
            SEARCH_HIGHLIGHT_BACKGROUND,
            SEARCH_CURRENT_HIGHLIGHT_BACKGROUND,
            SPLASH_GRADIENT_START,
            SPLASH_GRADIENT_END,
            CONSOLE_TEXT_AREA_BACKGROUND,
            CONSOLE_TEXT,
            CONSOLE_INFO,
            CONSOLE_DEBUG,
            CONSOLE_WARN,
            CONSOLE_ERROR,
            CONSOLE_CLASS_NAME,
            CONSOLE_METHOD_NAME,
            CONSOLE_TOOLBAR_BACKGROUND,
            CONSOLE_TOOLBAR_BORDER,
            CONSOLE_SELECTION_BACKGROUND,
            NOTIFICATION_BACKGROUND,
            NOTIFICATION_BORDER,
            NOTIFICATION_DIVIDER,
            NOTIFICATION_BODY_FOREGROUND
    );

    public static Color color(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }
}

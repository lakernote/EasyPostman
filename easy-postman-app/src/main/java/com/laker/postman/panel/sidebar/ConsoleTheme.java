package com.laker.postman.panel.sidebar;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class ConsoleTheme {
    Color matchCountForeground() {
        return ModernColors.getTextSecondary();
    }

    Color searchHighlightBackground() {
        return ModernColors.getSearchHighlightBackgroundColor();
    }

    Color searchCurrentHighlightBackground() {
        return ModernColors.getSearchCurrentHighlightBackgroundColor();
    }

    Color logForeground(ConsolePanel.LogType type) {
        return switch (type) {
            case ERROR -> ModernColors.getConsoleError();
            case SUCCESS -> ModernColors.getConsoleDebug();
            case WARN -> ModernColors.getConsoleWarn();
            case DEBUG -> ModernColors.getConsoleInfo();
            case TRACE -> ModernColors.getConsoleClassName();
            case CUSTOM -> ModernColors.getConsoleMethodName();
            case INFO -> ModernColors.getConsoleText();
        };
    }
}

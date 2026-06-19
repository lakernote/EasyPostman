package com.laker.postman.common.component.notification;

import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import lombok.experimental.UtilityClass;

@UtilityClass
class ToastTextFormatter {
    static String displayText(String message, boolean expanded) {
        if (message == null || message.isBlank()) {
            return "";
        }
        boolean foldable = isFoldable(message);
        if (!expanded && foldable) {
            return collapsedText(message);
        }
        return foldable ? message + " " + UiI18n.get(UiMessageKeys.NOTIFICATION_COLLAPSE) : message;
    }

    static boolean isFoldable(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return message.split("\n", -1).length > ToastStyle.COLLAPSED_MAX_LINES
                || message.length() > ToastStyle.COLLAPSED_MAX_LENGTH;
    }

    private String collapsedText(String message) {
        String[] lines = message.split("\n", -1);
        int show = Math.min(lines.length, ToastStyle.COLLAPSED_MAX_LINES);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < show; i++) {
            builder.append(lines[i]);
            if (i < show - 1) {
                builder.append("\n");
            }
        }
        builder.append("\u2026 ").append(UiI18n.get(UiMessageKeys.NOTIFICATION_EXPAND));
        return builder.toString();
    }
}

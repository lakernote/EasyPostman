package com.laker.postman.common.component.notification;

import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import lombok.experimental.UtilityClass;

@UtilityClass
class ToastTextFormatter {
    private static final String ELLIPSIS = "\u2026";

    static String displayText(String message, boolean expanded) {
        if (message == null || message.isBlank()) {
            return "";
        }
        boolean foldable = isFoldable(message);
        if (!expanded && foldable) {
            return collapsedText(message);
        }
        return message;
    }

    static String actionText(boolean expanded) {
        return UiI18n.get(expanded ? UiMessageKeys.NOTIFICATION_COLLAPSE : UiMessageKeys.NOTIFICATION_EXPAND);
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
            if (builder.length() >= ToastStyle.COLLAPSED_MAX_LENGTH) {
                break;
            }
            if (i > 0) {
                builder.append("\n");
            }
            int remaining = ToastStyle.COLLAPSED_MAX_LENGTH - builder.length();
            String line = lines[i];
            builder.append(line, 0, Math.min(line.length(), Math.max(0, remaining)));
            if (line.length() > remaining) {
                break;
            }
        }
        return trimTrailingWhitespace(builder).append(ELLIPSIS).toString();
    }

    private StringBuilder trimTrailingWhitespace(StringBuilder builder) {
        while (!builder.isEmpty() && Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder;
    }
}

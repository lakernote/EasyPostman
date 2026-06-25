package com.laker.postman.plugin.capture;

import com.laker.postman.common.component.ChipLabel;
import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.util.Locale;

@UtilityClass
class CaptureTableStyle {

    private static final long SLOW_REQUEST_MS = 1_000;
    private static final long VERY_SLOW_REQUEST_MS = 3_000;

    enum Tone {
        NORMAL,
        INFO,
        PRIMARY,
        SUCCESS,
        WARNING,
        FAILURE
    }

    static Tone durationTone(Object durationValue) {
        long durationMs = parseLong(durationValue);
        if (durationMs >= VERY_SLOW_REQUEST_MS) {
            return Tone.FAILURE;
        }
        if (durationMs >= SLOW_REQUEST_MS) {
            return Tone.WARNING;
        }
        return Tone.NORMAL;
    }

    static Tone methodTone(Object methodValue) {
        String method = methodValue == null ? "" : methodValue.toString().trim().toUpperCase(Locale.ROOT);
        return switch (method) {
            case "GET", "HEAD", "OPTIONS" -> Tone.INFO;
            case "POST" -> Tone.WARNING;
            case "PUT", "PATCH" -> Tone.PRIMARY;
            case "DELETE", "TLS", "CONNECT" -> Tone.FAILURE;
            default -> Tone.NORMAL;
        };
    }

    static boolean isSlow(Object durationValue) {
        return parseLong(durationValue) >= SLOW_REQUEST_MS;
    }

    static Color methodForegroundFor(Object methodValue) {
        return switch (methodTone(methodValue)) {
            case INFO -> ModernColors.getHttpMethodGet();
            case WARNING -> ModernColors.getHttpMethodPost();
            case PRIMARY -> ModernColors.getHttpMethodPut();
            case FAILURE -> ModernColors.getHttpMethodDelete();
            case SUCCESS -> ModernColors.getSuccess();
            case NORMAL -> ModernColors.getHttpMethodDefault();
        };
    }

    static Color durationForegroundFor(Object durationValue) {
        return switch (durationTone(durationValue)) {
            case WARNING -> ChipLabel.foregroundFor(ModernColors.getWarningDark());
            case FAILURE -> ChipLabel.foregroundFor(ModernColors.getError());
            default -> ModernColors.getTextPrimary();
        };
    }

    private static long parseLong(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}

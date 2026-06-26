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
        MUTED,
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

    static Tone sourceTone(Object sourceValue) {
        String source = sourceValue == null ? "" : sourceValue.toString().trim();
        if (source.isBlank()) {
            return Tone.MUTED;
        }
        if (source.contains("· PID ") || source.startsWith("PID ")) {
            return Tone.INFO;
        }
        return Tone.NORMAL;
    }

    static Tone bytesTone(Object bytesValue) {
        long bytes = parseLong(bytesValue);
        if (bytes <= 0) {
            return Tone.MUTED;
        }
        if (bytes >= 1024L * 1024L) {
            return Tone.FAILURE;
        }
        if (bytes >= 100L * 1024L) {
            return Tone.WARNING;
        }
        return Tone.NORMAL;
    }

    static Tone typeTone(Object typeValue) {
        String type = typeValue == null ? "" : typeValue.toString().trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "api", "json", "sse", "websocket" -> Tone.INFO;
            case "image", "css", "js", "font" -> Tone.PRIMARY;
            case "media" -> Tone.WARNING;
            case "other", "" -> Tone.MUTED;
            default -> Tone.NORMAL;
        };
    }

    static boolean isSlow(Object durationValue) {
        return parseLong(durationValue) >= SLOW_REQUEST_MS;
    }

    static Color sourceForegroundFor(Object sourceValue) {
        return switch (sourceTone(sourceValue)) {
            case INFO -> ChipLabel.foregroundFor(ModernColors.getInfo());
            case MUTED -> ModernColors.getTextSecondary();
            default -> ModernColors.getTextPrimary();
        };
    }

    static Color methodForegroundFor(Object methodValue) {
        return switch (methodTone(methodValue)) {
            case INFO -> ModernColors.getHttpMethodGet();
            case WARNING -> ModernColors.getHttpMethodPost();
            case PRIMARY -> ModernColors.getHttpMethodPut();
            case FAILURE -> ModernColors.getHttpMethodDelete();
            case SUCCESS -> ModernColors.getSuccess();
            case MUTED -> ModernColors.getTextSecondary();
            case NORMAL -> ModernColors.getHttpMethodDefault();
        };
    }

    static Color typeForegroundFor(Object typeValue) {
        return switch (typeTone(typeValue)) {
            case INFO -> ChipLabel.foregroundFor(ModernColors.getInfo());
            case PRIMARY -> ChipLabel.foregroundFor(ModernColors.getAccent());
            case WARNING -> ChipLabel.foregroundFor(ModernColors.getWarningDark());
            case MUTED -> ModernColors.getTextSecondary();
            default -> ModernColors.getTextPrimary();
        };
    }

    static Color durationForegroundFor(Object durationValue) {
        return switch (durationTone(durationValue)) {
            case WARNING -> ChipLabel.foregroundFor(ModernColors.getWarningDark());
            case FAILURE -> ChipLabel.foregroundFor(ModernColors.getError());
            case MUTED -> ModernColors.getTextSecondary();
            default -> ModernColors.getTextPrimary();
        };
    }

    static Color bytesForegroundFor(Object bytesValue) {
        return switch (bytesTone(bytesValue)) {
            case MUTED -> ModernColors.getTextSecondary();
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

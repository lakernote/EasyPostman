package com.laker.postman.plugin.capture;

import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
class CaptureValueFormat {

    private static final double KB = 1024.0;
    private static final double MB = KB * 1024.0;
    private static final double GB = MB * 1024.0;

    static String bytes(Object value) {
        long bytes = parseLong(value);
        if (bytes < 0) {
            return "-";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < MB) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / KB);
        }
        if (bytes < GB) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / MB);
        }
        return String.format(Locale.ROOT, "%.1f GB", bytes / GB);
    }

    static String duration(Object value) {
        long durationMs = parseLong(value);
        if (durationMs < 0) {
            return "-";
        }
        if (durationMs < 1000) {
            return durationMs + " ms";
        }
        if (durationMs < 10_000) {
            return String.format(Locale.ROOT, "%.2f s", durationMs / 1000.0);
        }
        return String.format(Locale.ROOT, "%.1f s", durationMs / 1000.0);
    }

    private static long parseLong(Object value) {
        if (value == null) {
            return -1;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}

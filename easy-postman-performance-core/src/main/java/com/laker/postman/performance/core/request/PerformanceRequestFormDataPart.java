package com.laker.postman.performance.core.request;

import lombok.Value;

@Value
public class PerformanceRequestFormDataPart {
    public static final String TYPE_TEXT = "Text";
    public static final String TYPE_FILE = "File";

    boolean enabled;
    String key;
    String type;
    String value;
    String description;

    public PerformanceRequestFormDataPart(boolean enabled, String key, String type, String value) {
        this(enabled, key, type, value, "");
    }

    public PerformanceRequestFormDataPart(boolean enabled, String key, String type, String value, String description) {
        this.enabled = enabled;
        this.key = blankToEmpty(key);
        this.type = normalizeType(type);
        this.value = blankToEmpty(value);
        this.description = blankToEmpty(description);
    }

    public boolean isFile() {
        return TYPE_FILE.equals(type);
    }

    public boolean isText() {
        return TYPE_TEXT.equals(type);
    }

    private static String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return TYPE_TEXT;
        }
        if (TYPE_FILE.equalsIgnoreCase(type.trim())) {
            return TYPE_FILE;
        }
        return TYPE_TEXT;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}

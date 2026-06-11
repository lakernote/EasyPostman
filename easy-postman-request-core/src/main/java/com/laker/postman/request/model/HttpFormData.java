package com.laker.postman.request.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Form-Data Parameter model with enabled state
 * Supports both text and file types
 */
@Data
@NoArgsConstructor
public class HttpFormData implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Type constants for form-data
     */
    public static final String TYPE_TEXT = "Text";
    public static final String TYPE_FILE = "File";

    private boolean enabled = true;
    private String key = "";
    private String type = TYPE_TEXT; // "Text" or "File"
    private String value = "";
    private String description = "";

    public HttpFormData(boolean enabled, String key, String type, String value) {
        this(enabled, key, type, value, "");
    }

    public HttpFormData(boolean enabled, String key, String type, String value, String description) {
        this.enabled = enabled;
        this.key = key;
        this.type = normalizeType(type);
        this.value = value;
        this.description = description;
    }

    /**
     * Normalize the type to ensure consistent capitalization
     *
     * @param type the type string (case-insensitive)
     * @return normalized type ("Text" or "File")
     */
    public static String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return TYPE_TEXT;
        }
        String normalized = type.trim();
        if (TYPE_FILE.equalsIgnoreCase(normalized)) {
            return TYPE_FILE;
        }
        return TYPE_TEXT;
    }

    /**
     * Check if this is a text type form-data
     *
     * @return true if type is Text
     */
    public boolean isText() {
        return TYPE_TEXT.equalsIgnoreCase(type);
    }

    /**
     * Check if this is a file type form-data
     *
     * @return true if type is File
     */
    public boolean isFile() {
        return TYPE_FILE.equalsIgnoreCase(type);
    }

    /**
     * Set the type with automatic normalization
     */
    public void setType(String type) {
        this.type = normalizeType(type);
    }
}

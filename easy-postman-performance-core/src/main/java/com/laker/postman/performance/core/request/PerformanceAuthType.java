package com.laker.postman.performance.core.request;

public enum PerformanceAuthType {
    INHERIT("Inherit auth from parent"),
    NONE("No Auth"),
    BASIC("Basic Auth"),
    BEARER("Bearer Token"),
    DIGEST("Digest Auth");

    private final String legacyValue;

    PerformanceAuthType(String legacyValue) {
        this.legacyValue = legacyValue;
    }

    public String getLegacyValue() {
        return legacyValue;
    }

    public static PerformanceAuthType fromLegacyValue(String value) {
        if (value == null || value.isBlank()) {
            return INHERIT;
        }
        for (PerformanceAuthType type : values()) {
            if (type.legacyValue.equals(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return INHERIT;
    }
}

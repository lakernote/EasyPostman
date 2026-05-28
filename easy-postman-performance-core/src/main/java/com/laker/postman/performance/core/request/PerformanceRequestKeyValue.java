package com.laker.postman.performance.core.request;

import lombok.Value;

@Value
public class PerformanceRequestKeyValue {
    boolean enabled;
    String key;
    String value;

    public PerformanceRequestKeyValue(boolean enabled, String key, String value) {
        this.enabled = enabled;
        this.key = blankToEmpty(key);
        this.value = blankToEmpty(value);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}

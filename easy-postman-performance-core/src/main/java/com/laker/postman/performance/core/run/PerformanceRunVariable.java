package com.laker.postman.performance.core.run;

import lombok.Value;

@Value
public class PerformanceRunVariable {
    boolean enabled;
    String key;
    String value;

    public PerformanceRunVariable(boolean enabled, String key, String value) {
        this.enabled = enabled;
        this.key = key == null ? "" : key;
        this.value = value == null ? "" : value;
    }
}

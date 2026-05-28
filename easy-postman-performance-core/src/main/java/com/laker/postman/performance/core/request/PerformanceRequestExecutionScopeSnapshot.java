package com.laker.postman.performance.core.request;

import lombok.Value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Value
public class PerformanceRequestExecutionScopeSnapshot {
    private static final PerformanceRequestExecutionScopeSnapshot EMPTY =
            new PerformanceRequestExecutionScopeSnapshot(Map.of());

    Map<String, String> groupVariables;

    public PerformanceRequestExecutionScopeSnapshot(Map<String, String> groupVariables) {
        this.groupVariables = Collections.unmodifiableMap(new LinkedHashMap<>(
                groupVariables == null ? Map.of() : groupVariables
        ));
    }

    public static PerformanceRequestExecutionScopeSnapshot empty() {
        return EMPTY;
    }

    public static PerformanceRequestExecutionScopeSnapshot fromGroupVariables(Map<String, String> groupVariables) {
        if (groupVariables == null || groupVariables.isEmpty()) {
            return EMPTY;
        }
        return new PerformanceRequestExecutionScopeSnapshot(groupVariables);
    }

    public String getGroupVariable(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return groupVariables.get(key);
    }
}

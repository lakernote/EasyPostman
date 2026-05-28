package com.laker.postman.service.variable;

import com.laker.postman.model.Variable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request-scoped variable data needed during send/script execution.
 * This keeps headless execution independent from Swing tree nodes.
 */
public final class RequestExecutionScope {

    private static final RequestExecutionScope EMPTY = new RequestExecutionScope(Map.of());

    private final Map<String, String> groupVariables;

    private RequestExecutionScope(Map<String, String> groupVariables) {
        this.groupVariables = Collections.unmodifiableMap(new LinkedHashMap<>(
                groupVariables == null ? Map.of() : groupVariables
        ));
    }

    public static RequestExecutionScope empty() {
        return EMPTY;
    }

    public static RequestExecutionScope fromGroupVariables(Map<String, String> groupVariables) {
        if (groupVariables == null || groupVariables.isEmpty()) {
            return EMPTY;
        }
        return new RequestExecutionScope(groupVariables);
    }

    public static RequestExecutionScope fromVariables(List<Variable> variables) {
        if (variables == null || variables.isEmpty()) {
            return EMPTY;
        }

        Map<String, String> variableMap = new LinkedHashMap<>();
        for (Variable variable : variables) {
            if (variable == null || variable.getKey() == null || variable.getKey().trim().isEmpty()) {
                continue;
            }
            variableMap.put(variable.getKey(), variable.getValue());
        }
        return fromGroupVariables(variableMap);
    }

    public String getGroupVariable(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return groupVariables.get(key);
    }

    public Map<String, String> getGroupVariables() {
        return groupVariables;
    }
}

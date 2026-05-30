package com.laker.postman.performance.core.run;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class PerformanceRunVariableSet {
    List<PerformanceRunVariable> variables;

    public PerformanceRunVariableSet(List<PerformanceRunVariable> variables) {
        this.variables = copyVariables(variables);
    }

    public static PerformanceRunVariableSet empty() {
        return new PerformanceRunVariableSet(List.of());
    }

    static List<PerformanceRunVariable> copyVariables(List<PerformanceRunVariable> variables) {
        List<PerformanceRunVariable> copy = new ArrayList<>();
        if (variables == null) {
            return List.of();
        }
        for (PerformanceRunVariable variable : variables) {
            if (variable != null) {
                copy.add(variable);
            }
        }
        return List.copyOf(copy);
    }
}

package com.laker.postman.performance.core.run;

import lombok.Value;

import java.util.List;

@Value
public class PerformanceRunEnvironment {
    String id;
    String name;
    List<PerformanceRunVariable> variables;

    public PerformanceRunEnvironment(String id, String name, List<PerformanceRunVariable> variables) {
        this.id = id;
        this.name = name;
        this.variables = PerformanceRunVariableSet.copyVariables(variables);
    }

    public static PerformanceRunEnvironment empty() {
        return new PerformanceRunEnvironment(null, null, List.of());
    }
}

package com.laker.postman.performance.core.controller;

public class ConditionData {
    public static final String DEFAULT_EXPRESSION = "true";

    public String expression = DEFAULT_EXPRESSION;

    public void normalize() {
        if (expression == null || expression.isBlank()) {
            expression = "";
            return;
        }
        expression = expression.trim();
    }
}

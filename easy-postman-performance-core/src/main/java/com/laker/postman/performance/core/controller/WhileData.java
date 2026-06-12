package com.laker.postman.performance.core.controller;

public class WhileData {
    public static final String DEFAULT_EXPRESSION = "false";
    public static final int MIN_INTERVAL_MS = 0;
    public static final int MAX_INTERVAL_MS = 3_600_000;
    public static final int MIN_TIMEOUT_MS = 0;
    public static final int MAX_TIMEOUT_MS = 86_400_000;
    public static final int MIN_MAX_ITERATIONS = 1;
    public static final int MAX_MAX_ITERATIONS = 100000;

    public String expression = DEFAULT_EXPRESSION;
    public int intervalMs = 1000;
    public int timeoutMs = 60000;
    public int maxIterations = 60;

    public void normalize() {
        if (expression == null || expression.isBlank()) {
            expression = "";
        } else {
            expression = expression.trim();
        }
        intervalMs = clamp(intervalMs, MIN_INTERVAL_MS, MAX_INTERVAL_MS);
        timeoutMs = clamp(timeoutMs, MIN_TIMEOUT_MS, MAX_TIMEOUT_MS);
        maxIterations = clamp(maxIterations, MIN_MAX_ITERATIONS, MAX_MAX_ITERATIONS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

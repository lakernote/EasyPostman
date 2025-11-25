package com.laker.postman.model;

public enum AssertionResult {
    PASS("✅"),

    FAIL("❌"),

    NO_TESTS("💨");

    private final String displayValue;

    AssertionResult(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
package com.laker.postman.functional.model;

import lombok.Getter;

@Getter
public enum AssertionResult {
    PASS("✅"),

    FAIL("❌"),

    NO_TESTS("💨");

    private final String displayValue;

    AssertionResult(String displayValue) {
        this.displayValue = displayValue;
    }

}

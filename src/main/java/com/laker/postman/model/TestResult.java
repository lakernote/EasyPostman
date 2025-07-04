package com.laker.postman.model;

public class TestResult {
    public final String name;
    public final boolean passed;
    public final String message;

    public TestResult(String name, boolean passed, String message) {
        this.name = name;
        this.passed = passed;
        this.message = message;
    }
}
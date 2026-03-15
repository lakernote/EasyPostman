package com.laker.postman.model.script;

public class TestResult {
    /**
     * 测试唯一标识
     */
    public String id;
    public final String name;
    public final boolean passed;
    public final String message;

    public TestResult(String name, boolean passed, String message) {
        this.name = name;
        this.passed = passed;
        this.message = message;
    }
}
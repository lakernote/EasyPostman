package com.laker.postman.panel.performance.controller;

public class LoopData {
    public static final int MIN_ITERATIONS = 1;
    public static final int MAX_ITERATIONS = 100000;

    public int iterations = 1;

    public void normalize() {
        iterations = Math.max(MIN_ITERATIONS, Math.min(MAX_ITERATIONS, iterations));
    }
}

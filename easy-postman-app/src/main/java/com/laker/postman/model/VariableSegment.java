package com.laker.postman.model;

public class VariableSegment {
    public final int start;
    public final int end;
    public final String name;

    public VariableSegment(int start, int end, String name) {
        this.start = start;
        this.end = end;
        this.name = name;
    }
}
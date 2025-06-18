package com.laker.postman.panel.batch;

// 结果行
public class ResultRow {
    String name;
    int code;
    long cost;
    String size;
    String error;

    public ResultRow(String name, int code, long cost, String size, String error) {
        this.name = name;
        this.code = code;
        this.cost = cost;
        this.size = size;
        this.error = error;
    }
}
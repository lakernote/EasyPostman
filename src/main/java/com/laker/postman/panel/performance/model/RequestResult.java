package com.laker.postman.panel.performance.model;

// 用于统计每个请求的结束时间和成功状态
public class RequestResult {
    public long endTime;
    public boolean success;

    public RequestResult(long endTime, boolean success) {
        this.endTime = endTime;
        this.success = success;
    }
}
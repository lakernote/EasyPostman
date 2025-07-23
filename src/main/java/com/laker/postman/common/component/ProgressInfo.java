package com.laker.postman.common.component;

// 进度信息结构体
public class ProgressInfo {
    public final int totalBytes;
    public final int contentLength;
    public final long elapsedMillis;

    public ProgressInfo(int totalBytes, int contentLength, long elapsedMillis) {
        this.totalBytes = totalBytes;
        this.contentLength = contentLength;
        this.elapsedMillis = elapsedMillis;
    }
}
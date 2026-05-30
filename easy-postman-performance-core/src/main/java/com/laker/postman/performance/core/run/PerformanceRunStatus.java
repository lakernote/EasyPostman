package com.laker.postman.performance.core.run;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceRunStatus {
    // 这些值会进入 CLI result.json 和 worker HTTP/JSON 协议，新增状态时先确认 master/GUI 聚合语义。
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String STOPPING = "STOPPING";
    public static final String STOPPED = "STOPPED";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String UNKNOWN = "UNKNOWN";

    public boolean isTerminal(String status) {
        return SUCCESS.equals(status) || FAILED.equals(status) || STOPPED.equals(status);
    }
}

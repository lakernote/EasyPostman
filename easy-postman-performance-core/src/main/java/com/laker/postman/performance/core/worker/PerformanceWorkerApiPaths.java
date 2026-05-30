package com.laker.postman.performance.core.worker;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceWorkerApiPaths {
    // worker server 和 master/client 共用同一份路径定义，避免文档或客户端遗漏版本化 base path。
    public static final String API_PREFIX = "/api/performance/v1";
    public static final String HEALTH = API_PREFIX + "/health";
    public static final String RUNS = API_PREFIX + "/runs";
    public static final String RESULT_SUFFIX = "/result";
    public static final String DETAILS_SUFFIX = "/details";
    public static final String STOP_SUFFIX = "/stop";

    public String run(String runId) {
        return RUNS + "/" + runId;
    }

    public String result(String runId) {
        return run(runId) + RESULT_SUFFIX;
    }

    public String details(String runId) {
        return run(runId) + DETAILS_SUFFIX;
    }

    public String stop(String runId) {
        return run(runId) + STOP_SUFFIX;
    }
}

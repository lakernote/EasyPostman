package com.laker.postman.performance.core.worker;

import lombok.Value;

@Value
public class PerformanceWorkerThreadGroupAssignment {
    String threadGroupPath;
    int threadGroupIndex;
    int firstVirtualUserIndex;
    int virtualUserCount;

    public PerformanceWorkerThreadGroupAssignment(String threadGroupPath,
                                                  int threadGroupIndex,
                                                  int firstVirtualUserIndex,
                                                  int virtualUserCount) {
        this.threadGroupPath = threadGroupPath == null ? "" : threadGroupPath;
        this.threadGroupIndex = Math.max(0, threadGroupIndex);
        this.firstVirtualUserIndex = Math.max(0, firstVirtualUserIndex);
        this.virtualUserCount = Math.max(0, virtualUserCount);
    }
}

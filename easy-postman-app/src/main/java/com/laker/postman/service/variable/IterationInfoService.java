package com.laker.postman.service.variable;

/**
 * Thread-scoped Postman-compatible runner metadata.
 */
public class IterationInfoService {

    private static final IterationInfoService INSTANCE = new IterationInfoService();
    private static final IterationInfo DEFAULT_INFO = new IterationInfo(0, 1);
    private static final ThreadLocal<IterationInfo> ITERATION_INFO = new ThreadLocal<>();

    private IterationInfoService() {
    }

    public static IterationInfoService getInstance() {
        return INSTANCE;
    }

    public IterationInfo getCurrentInfo() {
        IterationInfo info = ITERATION_INFO.get();
        return info == null ? DEFAULT_INFO : info;
    }

    public IterationInfo getCurrentContextInfo() {
        return ITERATION_INFO.get();
    }

    public void attachContextInfo(IterationInfo info) {
        if (info == null) {
            ITERATION_INFO.remove();
            return;
        }
        ITERATION_INFO.set(info);
    }

    public record IterationInfo(int iteration, int iterationCount) {
        public IterationInfo {
            iteration = Math.max(0, iteration);
            iterationCount = Math.max(0, iterationCount);
        }
    }
}

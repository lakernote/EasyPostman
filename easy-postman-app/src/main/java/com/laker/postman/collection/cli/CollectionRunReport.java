package com.laker.postman.collection.cli;

import java.util.List;

public record CollectionRunReport(
        String schemaVersion,
        String status,
        String collectionName,
        String collectionPath,
        long startTimeMs,
        long endTimeMs,
        long elapsedTimeMs,
        int iterations,
        int totalRequests,
        int passedRequests,
        int failedRequests,
        int totalTests,
        int passedTests,
        int failedTests,
        List<RequestResult> requests
) {
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    public CollectionRunReport {
        requests = requests == null ? List.of() : List.copyOf(requests);
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(status);
    }

    public record RequestResult(
            int iteration,
            String name,
            String path,
            String method,
            String url,
            String status,
            long durationMs,
            boolean passed,
            String error,
            List<TestCase> tests
    ) {
        public RequestResult {
            tests = tests == null ? List.of() : List.copyOf(tests);
        }
    }

    public record TestCase(String name, boolean passed, String message) {
    }
}

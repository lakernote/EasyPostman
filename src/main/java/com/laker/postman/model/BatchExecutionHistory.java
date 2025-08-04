package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 批量执行的历史记录
 * 记录所有轮次的执行结果
 */
public class BatchExecutionHistory {
    private final List<IterationResult> iterations = new ArrayList<>();
    private int totalIterations;
    private int totalRequests;
    private long startTime;
    private long endTime;

    public BatchExecutionHistory() {
        this.startTime = System.currentTimeMillis();
    }

    public void addIteration(IterationResult iteration) {
        iterations.add(iteration);
    }

    public void complete() {
        this.endTime = System.currentTimeMillis();
    }

    public List<IterationResult> getIterations() {
        return new ArrayList<>(iterations);
    }

    public int getTotalIterations() {
        return totalIterations;
    }

    public void setTotalIterations(int totalIterations) {
        this.totalIterations = totalIterations;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getExecutionTime() {
        return endTime - startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    /**
     * 单次迭代的执行结果
     */
    public static class IterationResult {
        private final int iterationIndex;
        private final Map<String, String> csvData;
        private final List<RequestResult> requestResults = new ArrayList<>();
        private final long startTime;
        private long endTime;

        public IterationResult(int iterationIndex, Map<String, String> csvData) {
            this.iterationIndex = iterationIndex;
            this.csvData = csvData;
            this.startTime = System.currentTimeMillis();
        }

        public void addRequestResult(RequestResult result) {
            requestResults.add(result);
        }

        public void complete() {
            this.endTime = System.currentTimeMillis();
        }

        public int getIterationIndex() {
            return iterationIndex;
        }

        public Map<String, String> getCsvData() {
            return csvData;
        }

        public List<RequestResult> getRequestResults() {
            return new ArrayList<>(requestResults);
        }

        public long getExecutionTime() {
            return endTime - startTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }
    }

    /**
     * 单个请求的执行结果
     */
    public static class RequestResult {
        private final String requestName;
        private final String method;
        private final String url;
        private final PreparedRequest req;
        private final HttpResponse response;
        private final long cost;
        private final String status;
        private final String assertion;
        private final List<TestResult> testResults;
        private final long timestamp;

        public RequestResult(String requestName, String method, String url,
                           PreparedRequest req, HttpResponse response, long cost, String status,
                           String assertion, List<TestResult> testResults) {
            this.requestName = requestName;
            this.method = method;
            this.url = url;
            this.req = req;
            this.response = response;
            this.cost = cost;
            this.status = status;
            this.assertion = assertion;
            this.testResults = testResults != null ? new ArrayList<>(testResults) : new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getRequestName() { return requestName; }
        public String getMethod() { return method; }
        public String getUrl() { return url; }
        public PreparedRequest getReq() { return req; }
        public HttpResponse getResponse() { return response; }
        public long getCost() { return cost; }
        public String getStatus() { return status; }
        public String getAssertion() { return assertion; }
        public List<TestResult> getTestResults() { return new ArrayList<>(testResults); }
        public long getTimestamp() { return timestamp; }
    }
}

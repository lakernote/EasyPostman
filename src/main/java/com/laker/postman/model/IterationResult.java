package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单次迭代的执行结果
 */
public class IterationResult {
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
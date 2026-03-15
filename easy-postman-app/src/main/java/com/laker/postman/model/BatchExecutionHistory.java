package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;

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


}
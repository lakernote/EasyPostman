package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class PerformanceResultRecorder {

    private final List<RequestResult> allRequestResults;
    private final Map<String, List<Long>> apiCostMap;
    private final Map<String, Integer> apiSuccessMap;
    private final Map<String, Integer> apiFailMap;
    private final Object statsLock;
    private final PerformanceResultTablePanel resultTablePanel;
    private final IntSupplier slowRequestThresholdSupplier;

    public PerformanceResultRecorder(List<RequestResult> allRequestResults,
                                     Map<String, List<Long>> apiCostMap,
                                     Map<String, Integer> apiSuccessMap,
                                     Map<String, Integer> apiFailMap,
                                     Object statsLock,
                                     PerformanceResultTablePanel resultTablePanel,
                                     IntSupplier slowRequestThresholdSupplier) {
        this.allRequestResults = allRequestResults;
        this.apiCostMap = apiCostMap;
        this.apiSuccessMap = apiSuccessMap;
        this.apiFailMap = apiFailMap;
        this.statsLock = statsLock;
        this.resultTablePanel = resultTablePanel;
        this.slowRequestThresholdSupplier = slowRequestThresholdSupplier;
    }

    public void record(PerformanceRequestExecutionResult executionResult, boolean efficientMode) {
        if (executionResult == null || executionResult.interrupted) {
            return;
        }

        long cost = resolveCostMs(executionResult);
        long endTime = resolveEndTime(executionResult, cost);
        boolean actualSuccess = ResultNodeInfo.isActuallySuccessful(
                executionResult.executionFailed,
                executionResult.response,
                executionResult.testResults
        );
        int slowRequestThresholdMs = slowRequestThresholdSupplier.getAsInt();

        updateStatistics(executionResult, cost, endTime, actualSuccess);

        if (!shouldRecordResult(efficientMode, actualSuccess, cost, slowRequestThresholdMs)) {
            return;
        }

        resultTablePanel.addResult(PerformanceResultNodeInfoMapper.toDisplayNodeInfo(executionResult));
    }

    static boolean shouldRecordResult(boolean efficientMode,
                                      boolean actualSuccess,
                                      long costMs,
                                      int slowRequestThresholdMs) {
        if (!efficientMode) {
            return true;
        }
        if (!actualSuccess) {
            return true;
        }
        return slowRequestThresholdMs > 0 && costMs >= slowRequestThresholdMs;
    }

    private long resolveCostMs(PerformanceRequestExecutionResult executionResult) {
        return executionResult.response == null ? executionResult.fallbackCostMs : executionResult.response.costMs;
    }

    private long resolveEndTime(PerformanceRequestExecutionResult executionResult, long costMs) {
        if (executionResult.response == null) {
            return executionResult.requestStartTime + costMs;
        }
        return executionResult.response.endTime > 0
                ? executionResult.response.endTime
                : executionResult.requestStartTime + costMs;
    }

    private void updateStatistics(PerformanceRequestExecutionResult executionResult,
                                  long costMs,
                                  long endTime,
                                  boolean actualSuccess) {
        synchronized (statsLock) {
            allRequestResults.add(new RequestResult(
                    executionResult.requestStartTime,
                    endTime,
                    actualSuccess,
                    executionResult.apiId
            ));
            apiCostMap
                    .computeIfAbsent(executionResult.apiId, key -> Collections.synchronizedList(new ArrayList<>()))
                    .add(costMs);
            if (actualSuccess) {
                apiSuccessMap.merge(executionResult.apiId, 1, Integer::sum);
            } else {
                apiFailMap.merge(executionResult.apiId, 1, Integer::sum);
            }
        }
    }

}

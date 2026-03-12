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

        long cost = executionResult.response == null ? executionResult.fallbackCostMs : executionResult.response.costMs;
        long endTime = executionResult.requestStartTime + cost;
        if (executionResult.response != null) {
            endTime = executionResult.response.endTime > 0
                    ? executionResult.response.endTime
                    : executionResult.requestStartTime + cost;
        }

        executionResult.request.simplify();
        if (executionResult.response != null) {
            executionResult.response.simplify();
            if (!executionResult.request.collectEventInfo) {
                executionResult.response.httpEventInfo = null;
            }
        }

        ResultNodeInfo resultNodeInfo = new ResultNodeInfo(
                executionResult.apiName,
                executionResult.errorMsg,
                executionResult.request,
                executionResult.response,
                executionResult.testResults,
                executionResult.executionFailed
        );
        boolean actualSuccess = resultNodeInfo.isActuallySuccessful();

        synchronized (statsLock) {
            allRequestResults.add(new RequestResult(executionResult.requestStartTime, endTime, actualSuccess, executionResult.apiId));
            apiCostMap.computeIfAbsent(executionResult.apiId, key -> Collections.synchronizedList(new ArrayList<>())).add(cost);
            if (actualSuccess) {
                apiSuccessMap.merge(executionResult.apiId, 1, Integer::sum);
            } else {
                apiFailMap.merge(executionResult.apiId, 1, Integer::sum);
            }
        }

        resultTablePanel.addResult(resultNodeInfo, efficientMode, slowRequestThresholdSupplier.getAsInt());
    }
}

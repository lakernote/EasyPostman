package com.laker.postman.performance.plan;

import com.laker.postman.performance.model.PerformanceTreeNode;

import java.util.ArrayList;
import java.util.List;

public final class PerformanceTestPlanNode {
    private final PerformanceTreeNode data;
    private final List<PerformanceTestPlanNode> children = new ArrayList<>();

    public PerformanceTestPlanNode(PerformanceTreeNode data) {
        this.data = data;
    }

    public void add(PerformanceTestPlanNode child) {
        children.add(child);
    }

    public PerformanceTreeNode getUserObject() {
        return data;
    }

    public PerformancePlanNode toPlanNode() {
        if (data == null) {
            return null;
        }
        return PerformancePlanNode.builder()
                .name(data.name)
                .type(data.type)
                .enabled(data.enabled)
                .threadGroupData(data.threadGroupData)
                .csvDataSetData(data.csvDataSetData)
                .loopData(data.loopData)
                .conditionData(data.conditionData)
                .whileData(data.whileData)
                .httpRequestItem(data.httpRequestItem)
                .requestSnapshot(data.requestSnapshot)
                .assertionData(data.assertionData)
                .extractorData(data.extractorData)
                .timerData(data.timerData)
                .ssePerformanceData(data.ssePerformanceData)
                .webSocketPerformanceData(data.webSocketPerformanceData)
                .requestExecutionScope(data.requestExecutionScope)
                .children(children.stream()
                        .map(PerformanceTestPlanNode::toPlanNode)
                        .toList())
                .build();
    }
}

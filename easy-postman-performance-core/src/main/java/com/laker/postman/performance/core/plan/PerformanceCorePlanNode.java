package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public class PerformanceCorePlanNode {
    String name;
    NodeType type;
    boolean enabled;
    ThreadGroupData threadGroupData;
    CsvDataSetData csvDataSetData;
    LoopData loopData;
    ConditionData conditionData;
    WhileData whileData;
    PerformanceRequestSnapshot requestSnapshot;
    AssertionData assertionData;
    ExtractorData extractorData;
    TimerData timerData;
    SsePerformanceData ssePerformanceData;
    WebSocketPerformanceData webSocketPerformanceData;
    List<PerformanceCorePlanNode> children;

    @Builder
    public PerformanceCorePlanNode(String name,
                                   NodeType type,
                                   Boolean enabled,
                                   ThreadGroupData threadGroupData,
                                   CsvDataSetData csvDataSetData,
                                   LoopData loopData,
                                   ConditionData conditionData,
                                   WhileData whileData,
                                   PerformanceRequestSnapshot requestSnapshot,
                                   AssertionData assertionData,
                                   ExtractorData extractorData,
                                   TimerData timerData,
                                   SsePerformanceData ssePerformanceData,
                                   WebSocketPerformanceData webSocketPerformanceData,
                                   List<PerformanceCorePlanNode> children) {
        this.name = name;
        this.type = type;
        this.enabled = enabled == null || enabled;
        this.threadGroupData = PerformancePlanCoreDataCopies.copyThreadGroupData(threadGroupData);
        this.csvDataSetData = PerformancePlanCoreDataCopies.copyCsvDataSetData(csvDataSetData);
        this.loopData = PerformancePlanCoreDataCopies.copyLoopData(loopData);
        this.conditionData = PerformancePlanCoreDataCopies.copyConditionData(conditionData);
        this.whileData = PerformancePlanCoreDataCopies.copyWhileData(whileData);
        this.requestSnapshot = canonicalRequestSnapshot(name, type, requestSnapshot);
        this.assertionData = PerformancePlanCoreDataCopies.copyAssertionData(assertionData);
        this.extractorData = PerformancePlanCoreDataCopies.copyExtractorData(extractorData);
        this.timerData = PerformancePlanCoreDataCopies.copyTimerData(timerData);
        this.ssePerformanceData = PerformancePlanCoreDataCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanCoreDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
    }

    public PerformanceRequestSnapshot getRequestSnapshot() {
        return copyRequestSnapshot(requestSnapshot);
    }

    private static PerformanceRequestSnapshot copyRequestSnapshot(PerformanceRequestSnapshot source) {
        return source == null ? null : source.toBuilder().build();
    }

    private static PerformanceRequestSnapshot canonicalRequestSnapshot(String nodeName,
                                                                       NodeType type,
                                                                       PerformanceRequestSnapshot source) {
        if (type != NodeType.REQUEST || source == null) {
            return copyRequestSnapshot(source);
        }
        if (nodeName == null || nodeName.trim().isEmpty()) {
            return copyRequestSnapshot(source);
        }
        return source.toBuilder()
                .name(nodeName.trim())
                .build();
    }
}

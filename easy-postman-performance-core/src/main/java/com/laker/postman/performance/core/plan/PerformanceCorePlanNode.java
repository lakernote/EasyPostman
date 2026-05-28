package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.LoopData;
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
    PerformanceRequestSnapshot requestSnapshot;
    AssertionData assertionData;
    ExtractorData extractorData;
    TimerData timerData;
    SsePerformanceData ssePerformanceData;
    WebSocketPerformanceData webSocketPerformanceData;
    boolean requestInheritanceSnapshot;
    List<PerformanceCorePlanNode> children;

    @Builder
    public PerformanceCorePlanNode(String name,
                                   NodeType type,
                                   Boolean enabled,
                                   ThreadGroupData threadGroupData,
                                   CsvDataSetData csvDataSetData,
                                   LoopData loopData,
                                   PerformanceRequestSnapshot requestSnapshot,
                                   AssertionData assertionData,
                                   ExtractorData extractorData,
                                   TimerData timerData,
                                   SsePerformanceData ssePerformanceData,
                                   WebSocketPerformanceData webSocketPerformanceData,
                                   Boolean requestInheritanceSnapshot,
                                   List<PerformanceCorePlanNode> children) {
        this.name = name;
        this.type = type;
        this.enabled = enabled == null || enabled;
        this.threadGroupData = PerformancePlanCoreDataCopies.copyThreadGroupData(threadGroupData);
        this.csvDataSetData = PerformancePlanCoreDataCopies.copyCsvDataSetData(csvDataSetData);
        this.loopData = PerformancePlanCoreDataCopies.copyLoopData(loopData);
        this.requestSnapshot = copyRequestSnapshot(requestSnapshot);
        this.assertionData = PerformancePlanCoreDataCopies.copyAssertionData(assertionData);
        this.extractorData = PerformancePlanCoreDataCopies.copyExtractorData(extractorData);
        this.timerData = PerformancePlanCoreDataCopies.copyTimerData(timerData);
        this.ssePerformanceData = PerformancePlanCoreDataCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanCoreDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        this.requestInheritanceSnapshot = requestInheritanceSnapshot != null && requestInheritanceSnapshot;
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
    }

    public PerformanceRequestSnapshot getRequestSnapshot() {
        return copyRequestSnapshot(requestSnapshot);
    }

    private static PerformanceRequestSnapshot copyRequestSnapshot(PerformanceRequestSnapshot source) {
        return source == null ? null : source.toBuilder().build();
    }
}

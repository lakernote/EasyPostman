package com.laker.postman.panel.performance.plan;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.extractor.ExtractorData;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.timer.TimerData;
import com.laker.postman.util.JsonUtil;

public interface PerformancePlanElement {
    String getName();

    NodeType getType();
}

final class PerformancePlanNodeCopies {
    private PerformancePlanNodeCopies() {
    }

    static ThreadGroupData copyThreadGroupData(ThreadGroupData source) {
        return JsonUtil.deepCopy(source, ThreadGroupData.class);
    }

    static LoopData copyLoopData(LoopData source) {
        return JsonUtil.deepCopy(source, LoopData.class);
    }

    static TimerData copyTimerData(TimerData source) {
        return JsonUtil.deepCopy(source, TimerData.class);
    }

    static HttpRequestItem copyHttpRequestItem(HttpRequestItem source) {
        return JsonUtil.deepCopy(source, HttpRequestItem.class);
    }

    static AssertionData copyAssertionData(AssertionData source) {
        return JsonUtil.deepCopy(source, AssertionData.class);
    }

    static ExtractorData copyExtractorData(ExtractorData source) {
        return JsonUtil.deepCopy(source, ExtractorData.class);
    }

    static SsePerformanceData copySsePerformanceData(SsePerformanceData source) {
        return JsonUtil.deepCopy(source, SsePerformanceData.class);
    }

    static WebSocketPerformanceData copyWebSocketPerformanceData(WebSocketPerformanceData source) {
        return JsonUtil.deepCopy(source, WebSocketPerformanceData.class);
    }
}

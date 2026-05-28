package com.laker.postman.panel.performance.plan;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
class PerformancePlanDataCopies {

    ThreadGroupData copyThreadGroupData(ThreadGroupData source) {
        return JsonUtil.deepCopy(source, ThreadGroupData.class);
    }

    CsvDataSetData copyCsvDataSetData(CsvDataSetData source) {
        return JsonUtil.deepCopy(source, CsvDataSetData.class);
    }

    LoopData copyLoopData(LoopData source) {
        return JsonUtil.deepCopy(source, LoopData.class);
    }

    TimerData copyTimerData(TimerData source) {
        return JsonUtil.deepCopy(source, TimerData.class);
    }

    HttpRequestItem copyHttpRequestItem(HttpRequestItem source) {
        return JsonUtil.deepCopy(source, HttpRequestItem.class);
    }

    AssertionData copyAssertionData(AssertionData source) {
        return JsonUtil.deepCopy(source, AssertionData.class);
    }

    ExtractorData copyExtractorData(ExtractorData source) {
        return JsonUtil.deepCopy(source, ExtractorData.class);
    }

    SsePerformanceData copySsePerformanceData(SsePerformanceData source) {
        return JsonUtil.deepCopy(source, SsePerformanceData.class);
    }

    WebSocketPerformanceData copyWebSocketPerformanceData(WebSocketPerformanceData source) {
        return JsonUtil.deepCopy(source, WebSocketPerformanceData.class);
    }

    RequestExecutionScope copyRequestExecutionScope(RequestExecutionScope source) {
        return source == null ? null : RequestExecutionScope.fromGroupVariables(source.getGroupVariables());
    }
}

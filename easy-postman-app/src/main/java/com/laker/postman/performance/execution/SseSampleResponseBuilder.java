package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.model.SsePerformanceData;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.LinkedHashMap;

@UtilityClass
class SseSampleResponseBuilder {

    void addSummaryHeaders(HttpResponse response,
                           SsePerformanceData config,
                           int eventCount,
                           int matchedMessageCount,
                           long firstEventLatencyMs,
                           String lastEventId,
                           String lastEventType,
                           String errorMessage) {
        if (response.headers == null) {
            response.headers = new LinkedHashMap<>();
        }
        SsePerformanceData headerConfig = config == null ? new SsePerformanceData() : config;
        response.addHeader("X-Easy-SSE-Mode", Collections.singletonList(headerConfig.completionMode.name()));
        response.addHeader("X-Easy-SSE-Event-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(headerConfig.eventNameFilter, "")));
        response.addHeader("X-Easy-SSE-Message-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(headerConfig.messageFilter, "")));
        response.addHeader("X-Easy-SSE-Event-Count", Collections.singletonList(String.valueOf(eventCount)));
        response.addHeader("X-Easy-SSE-Message-Count", Collections.singletonList(String.valueOf(matchedMessageCount)));
        String firstEventLatencyHeader = firstEventLatencyMs >= 0 ? String.valueOf(firstEventLatencyMs) : "";
        response.addHeader("X-Easy-SSE-First-Event-Latency-Ms", Collections.singletonList(firstEventLatencyHeader));
        response.addHeader("X-Easy-SSE-Event-Id", Collections.singletonList(CharSequenceUtil.blankToDefault(lastEventId, "")));
        response.addHeader("X-Easy-SSE-Event-Type", Collections.singletonList(CharSequenceUtil.blankToDefault(lastEventType, "")));
        if (CharSequenceUtil.isNotBlank(errorMessage)) {
            response.addHeader("X-Easy-SSE-Error", Collections.singletonList(errorMessage));
        }
    }
}

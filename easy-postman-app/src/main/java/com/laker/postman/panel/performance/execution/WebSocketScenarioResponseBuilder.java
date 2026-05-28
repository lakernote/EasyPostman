package com.laker.postman.panel.performance.execution;

import com.laker.postman.performance.core.model.WebSocketPerformanceData;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@UtilityClass
class WebSocketScenarioResponseBuilder {

    void addSummaryHeaders(HttpResponse response,
                           WebSocketPerformanceData config,
                           int receivedMessages,
                           int sentMessages,
                           int matchedMessages,
                           long firstMessageLatencyMs,
                           String lastMessage,
                           String errorMessage) {
        if (response.headers == null) {
            response.headers = new LinkedHashMap<>();
        }
        WebSocketPerformanceData headerConfig = config == null ? new WebSocketPerformanceData() : config;
        response.addHeader("X-Easy-WS-Send-Mode", Collections.singletonList(headerConfig.sendMode.name()));
        WebSocketPerformanceData.SendContentSource contentSource = headerConfig.sendContentSource != null
                ? headerConfig.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        response.addHeader("X-Easy-WS-Send-Content-Source", Collections.singletonList(contentSource.name()));
        response.addHeader("X-Easy-WS-Send-Count-Configured", Collections.singletonList(String.valueOf(Math.max(1, headerConfig.sendCount))));
        response.addHeader("X-Easy-WS-Send-Interval-Ms", Collections.singletonList(String.valueOf(Math.max(0, headerConfig.sendIntervalMs))));
        WebSocketPerformanceData.CompletionMode completionMode = headerConfig.completionMode != null
                ? headerConfig.completionMode
                : WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE;
        response.addHeader("X-Easy-WS-Mode", Collections.singletonList(completionMode.name()));
        response.addHeader("X-Easy-WS-Message-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(headerConfig.messageFilter, "")));
        response.addHeader("X-Easy-WS-Received-Count", Collections.singletonList(String.valueOf(receivedMessages)));
        response.addHeader("X-Easy-WS-Sent-Count", Collections.singletonList(String.valueOf(sentMessages)));
        response.addHeader("X-Easy-WS-Message-Count", Collections.singletonList(String.valueOf(matchedMessages)));
        response.addHeader("X-Easy-WS-First-Message-Latency-Ms", Collections.singletonList(firstMessageLatencyMs >= 0 ? String.valueOf(firstMessageLatencyMs) : ""));
        response.addHeader("X-Easy-WS-Last-Message", Collections.singletonList(CharSequenceUtil.blankToDefault(lastMessage, "")));
        if (CharSequenceUtil.isNotBlank(errorMessage)) {
            response.addHeader("X-Easy-WS-Error", Collections.singletonList(errorMessage));
        }
    }

    String buildResponseBody(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (String message : messages) {
            String value = message == null ? "" : message;
            buffer.append(value).append("\n\n");
        }
        return buffer.toString();
    }
}

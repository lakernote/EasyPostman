package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import lombok.experimental.UtilityClass;

@UtilityClass
class SseSampleMatcher {

    boolean matchesEvent(SsePerformanceData config, String eventType) {
        if (!SsePerformanceData.usesEventNameFilter(config.completionMode)) {
            return true;
        }
        String filter = config.eventNameFilter;
        return CharSequenceUtil.isBlank(filter) || CharSequenceUtil.equals(filter.trim(), eventType);
    }

    boolean matchesPayload(SsePerformanceData config, String data) {
        if (config.completionMode != SsePerformanceData.CompletionMode.UNTIL_MATCH) {
            return true;
        }
        String filter = config.messageFilter;
        return CharSequenceUtil.isBlank(filter) || (data != null && data.contains(filter.trim()));
    }
}

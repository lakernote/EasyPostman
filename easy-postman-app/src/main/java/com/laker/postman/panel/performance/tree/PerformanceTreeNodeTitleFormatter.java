package com.laker.postman.panel.performance.tree;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.extractor.ExtractorData;
import com.laker.postman.panel.performance.extractor.ExtractorType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import java.util.StringJoiner;

@UtilityClass
public class PerformanceTreeNodeTitleFormatter {

    public String sseAwaitTitle(SsePerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_AWAIT);
        }
        StringJoiner joiner = new StringJoiner(
                " | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_AWAIT) + " [",
                "]"
        );
        SsePerformanceData.CompletionMode mode = data.completionMode != null
                ? data.completionMode
                : SsePerformanceData.CompletionMode.FIRST_MESSAGE;
        joiner.add(sseCompletionModeLabel(mode));
        switch (mode) {
            case FIRST_MESSAGE, MATCHED_MESSAGE -> joiner.add(formatDuration(data.firstMessageTimeoutMs));
            case MESSAGE_COUNT -> {
                joiner.add(String.valueOf(Math.max(1, data.targetMessageCount)));
                joiner.add(formatDuration(data.holdConnectionMs));
            }
            case FIXED_DURATION, STREAM_CLOSED -> joiner.add(formatDuration(data.holdConnectionMs));
        }
        if (mode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE
                && CharSequenceUtil.isNotBlank(data.messageFilter)) {
            joiner.add("contains=" + data.messageFilter.trim());
        }
        if (SsePerformanceData.usesEventNameFilter(mode)
                && CharSequenceUtil.isNotBlank(data.eventNameFilter)) {
            joiner.add("event=" + data.eventNameFilter.trim());
        }
        return joiner.toString();
    }

    public String loopTitle(LoopData data) {
        LoopData normalizedData = data != null ? data : new LoopData();
        normalizedData.normalize();
        return I18nUtil.getMessage(MessageKeys.PERFORMANCE_LOOP_NODE)
                + " [" + normalizedData.iterations + "x]";
    }

    public String extractorTitle(ExtractorData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_NODE);
        }
        ExtractorType type = ExtractorType.fromStorageValue(data.type);
        String variableName = CharSequenceUtil.blankToDefault(data.variableName, "?");
        String expression = CharSequenceUtil.blankToDefault(data.expression, "");
        StringJoiner joiner = new StringJoiner(
                " | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_EXTRACTOR_NODE) + " [",
                "]"
        );
        joiner.add(type.displayName());
        joiner.add(variableName.trim());
        if (CharSequenceUtil.isNotBlank(expression)) {
            joiner.add(expression.trim());
        }
        return joiner.toString();
    }

    public String webSocketSendTitle(WebSocketPerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND);
        }
        WebSocketPerformanceData.SendMode sendMode = data.sendMode != null
                ? data.sendMode
                : WebSocketPerformanceData.SendMode.NONE;
        String modeLabel = switch (sendMode) {
            case NONE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_NONE);
            case REQUEST_BODY_ON_CONNECT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY);
            case REQUEST_BODY_REPEAT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT);
        };
        StringJoiner joiner = new StringJoiner(
                " | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND) + " [",
                "]"
        );
        joiner.add(modeLabel);
        WebSocketPerformanceData.SendContentSource contentSource = data.sendContentSource != null
                ? data.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        if (sendMode != WebSocketPerformanceData.SendMode.NONE
                && contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT) {
            joiner.add(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_CONTENT_CUSTOM_TEXT));
        }
        if (sendMode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT) {
            joiner.add(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_WS_SEND_PER_LOOP_COUNT,
                    Math.max(1, data.sendCount)
            ));
            joiner.add(formatDuration(Math.max(0, data.sendIntervalMs)));
        }
        return joiner.toString();
    }

    public String webSocketAwaitTitle(WebSocketPerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT);
        }
        StringJoiner joiner = new StringJoiner(
                " | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT) + " [",
                "]"
        );
        WebSocketPerformanceData.CompletionMode mode = data.completionMode != null
                ? data.completionMode
                : WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE;
        joiner.add(webSocketCompletionModeLabel(mode));
        switch (mode) {
            case SINGLE_MESSAGE, UNTIL_MATCH -> joiner.add(formatDuration(data.firstMessageTimeoutMs));
            case MESSAGE_COUNT -> {
                joiner.add(String.valueOf(Math.max(1, data.targetMessageCount)));
                joiner.add(formatDuration(data.firstMessageTimeoutMs));
            }
            case FIXED_DURATION -> joiner.add(formatDuration(data.holdConnectionMs));
        }
        if (WebSocketPerformanceData.usesMessageFilter(mode)
                && CharSequenceUtil.isNotBlank(data.messageFilter)) {
            joiner.add("contains=" + data.messageFilter.trim());
        }
        return joiner.toString();
    }

    public String formatDuration(int durationMs) {
        if (durationMs >= 1000 && durationMs % 1000 == 0) {
            return (durationMs / 1000) + "s";
        }
        return durationMs + "ms";
    }

    private String sseCompletionModeLabel(SsePerformanceData.CompletionMode mode) {
        SsePerformanceData.CompletionMode normalizedMode = mode != null
                ? mode
                : SsePerformanceData.CompletionMode.FIRST_MESSAGE;
        return switch (normalizedMode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIRST_MESSAGE);
            case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_MESSAGE_COUNT);
            case STREAM_CLOSED -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_STREAM_CLOSED);
        };
    }

    private String webSocketCompletionModeLabel(WebSocketPerformanceData.CompletionMode mode) {
        WebSocketPerformanceData.CompletionMode normalizedMode = mode != null
                ? mode
                : WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE;
        return switch (normalizedMode) {
            case SINGLE_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE);
            case UNTIL_MATCH -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT);
        };
    }
}

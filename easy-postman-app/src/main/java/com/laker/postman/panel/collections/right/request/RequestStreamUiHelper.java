package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.MessageType;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class RequestStreamUiHelper {
    private final ResponsePanel responsePanel;
    private final DateTimeFormatter timeFormatter;

    RequestStreamUiHelper(ResponsePanel responsePanel, DateTimeFormatter timeFormatter) {
        this.responsePanel = responsePanel;
        this.timeFormatter = timeFormatter;
    }

    void appendWebSocketMessage(MessageType type, String text) {
        appendWebSocketMessage(type, text, null);
    }

    void appendWebSocketMessage(MessageType type, String text, List<TestResult> testResults) {
        if (responsePanel.getProtocol().isWebSocketProtocol() && responsePanel.getWebSocketResponsePanel() != null) {
            String timestamp = currentTimestamp();
            responsePanel.getWebSocketResponsePanel().addMessage(type, timestamp, text, testResults);
        }
    }

    void appendWebSocketRawEvent(StringBuilder webSocketBodyBuilder, MessageType type, String text) {
        if (webSocketBodyBuilder == null) {
            return;
        }
        String normalizedText = normalizeWebSocketTranscriptText(text);
        webSocketBodyBuilder
                .append('[')
                .append(currentTimestamp())
                .append("] ")
                .append(type.display)
                .append(": ")
                .append(normalizedText)
                .append('\n');
    }

    void appendSseMessage(MessageType type, String eventId, String eventType, Long retryMs,
                          String text, List<TestResult> testResults) {
        if (responsePanel.getSseResponsePanel() == null) {
            return;
        }
        String timestamp = currentTimestamp();
        responsePanel.getSseResponsePanel().addMessage(type, timestamp, eventId, eventType, retryMs, text, testResults);
    }

    void appendSseRawEvent(StringBuilder sseBodyBuilder, String id, String type, String data) {
        if (data == null) {
            return;
        }
        if (id != null && !id.isBlank()) {
            sseBodyBuilder.append("id: ").append(id).append('\n');
        }
        if (type != null && !type.isBlank()) {
            sseBodyBuilder.append("event: ").append(type).append('\n');
        }
        for (String line : data.split("\\R", -1)) {
            sseBodyBuilder.append("data: ").append(line).append('\n');
        }
        sseBodyBuilder.append('\n');
    }

    void finalizeSseResponse(HttpResponse response, StringBuilder sseBodyBuilder, long startTime) {
        if (response == null) {
            return;
        }
        response.isSse = true;
        response.body = sseBodyBuilder.toString();
        response.bodySize = response.body.getBytes(StandardCharsets.UTF_8).length;
        response.costMs = System.currentTimeMillis() - startTime;
        response.endTime = System.currentTimeMillis();
    }

    void finalizeWebSocketResponse(HttpResponse response, StringBuilder webSocketBodyBuilder, long startTime) {
        if (response == null) {
            return;
        }
        response.body = webSocketBodyBuilder != null ? webSocketBodyBuilder.toString() : "";
        response.bodySize = response.body.getBytes(StandardCharsets.UTF_8).length;
        response.costMs = System.currentTimeMillis() - startTime;
        response.endTime = System.currentTimeMillis();
    }

    private String currentTimestamp() {
        return LocalTime.now().format(timeFormatter);
    }

    private String normalizeWebSocketTranscriptText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = trimEdgeNewlines(normalized);
        return normalized.replace("\n", "\n  ");
    }

    private String trimEdgeNewlines(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && text.charAt(start) == '\n') {
            start++;
        }
        while (end > start && text.charAt(end - 1) == '\n') {
            end--;
        }
        return text.substring(start, end);
    }
}

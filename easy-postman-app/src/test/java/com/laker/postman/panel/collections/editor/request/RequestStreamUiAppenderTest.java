package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.stream.MessageType;
import org.testng.annotations.Test;

import java.time.format.DateTimeFormatter;

import static org.testng.Assert.assertTrue;

public class RequestStreamUiAppenderTest {

    @Test(description = "WebSocket 消息流收口到响应体后不应为空")
    public void testFinalizeWebSocketResponseShouldPopulateBody() {
        RequestStreamUiAppender appender = new RequestStreamUiAppender(null, DateTimeFormatter.ofPattern("HH:mm:ss"));
        HttpResponse response = new HttpResponse();
        StringBuilder bodyBuilder = new StringBuilder();
        long queueStartMs = System.currentTimeMillis() - 10;

        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.CONNECTED, "Switching Protocols");
        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, "{\"ok\":true}");
        appender.finalizeWebSocketResponse(response, bodyBuilder, queueStartMs);

        assertTrue(response.body.contains("CONNECTED") || response.body.contains(StreamMessageUiMetadata.display(MessageType.CONNECTED)));
        assertTrue(response.body.contains("RECEIVED") || response.body.contains(StreamMessageUiMetadata.display(MessageType.RECEIVED)));
        assertTrue(response.bodySize > 0);
        assertTrue(response.endTime > 0);
        assertTrue(response.costMs >= 0);
    }

    @Test(description = "WebSocket transcript 应去掉首尾多余空行，避免 history body 第一行错位")
    public void testAppendWebSocketRawEventShouldTrimEdgeNewlines() {
        RequestStreamUiAppender appender = new RequestStreamUiAppender(null, DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder bodyBuilder = new StringBuilder();

        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.CONNECTED, "Switching Protocols\n");
        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, "line1\nline2");

        String transcript = bodyBuilder.toString();
        assertTrue(!transcript.contains("Protocols\n\n["));
        assertTrue(transcript.contains("line1\n  line2"));
    }

    @Test(description = "缺少 ResponsePanel 时流式 UI 追加应安全降级为 no-op")
    public void testAppendUiMessageShouldIgnoreMissingResponsePanel() {
        RequestStreamUiAppender appender = new RequestStreamUiAppender(null, DateTimeFormatter.ofPattern("HH:mm:ss"));

        appender.appendWebSocketMessage(MessageType.RECEIVED, "hello");
        appender.appendSseMessage(MessageType.RECEIVED, null, "message", null, "hello", null);
        appender.appendSseRawEvent(null, null, "message", "hello");
        appender.finalizeSseResponse(new HttpResponse(), null, System.currentTimeMillis());
    }
}

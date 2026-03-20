package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.MessageType;
import org.testng.annotations.Test;

import java.time.format.DateTimeFormatter;

import static org.testng.Assert.assertTrue;

public class RequestStreamUiHelperTest {

    @Test(description = "WebSocket 消息流收口到响应体后不应为空")
    public void testFinalizeWebSocketResponseShouldPopulateBody() {
        RequestStreamUiHelper helper = new RequestStreamUiHelper(null, DateTimeFormatter.ofPattern("HH:mm:ss"));
        HttpResponse response = new HttpResponse();
        StringBuilder bodyBuilder = new StringBuilder();
        long startTime = System.currentTimeMillis() - 10;

        helper.appendWebSocketRawEvent(bodyBuilder, MessageType.CONNECTED, "Switching Protocols");
        helper.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, "{\"ok\":true}");
        helper.finalizeWebSocketResponse(response, bodyBuilder, startTime);

        assertTrue(response.body.contains("CONNECTED") || response.body.contains(MessageType.CONNECTED.display));
        assertTrue(response.body.contains("RECEIVED") || response.body.contains(MessageType.RECEIVED.display));
        assertTrue(response.bodySize > 0);
        assertTrue(response.endTime > 0);
        assertTrue(response.costMs >= 0);
    }

    @Test(description = "WebSocket transcript 应去掉首尾多余空行，避免 history body 第一行错位")
    public void testAppendWebSocketRawEventShouldTrimEdgeNewlines() {
        RequestStreamUiHelper helper = new RequestStreamUiHelper(null, DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder bodyBuilder = new StringBuilder();

        helper.appendWebSocketRawEvent(bodyBuilder, MessageType.CONNECTED, "Switching Protocols\n");
        helper.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, "line1\nline2");

        String transcript = bodyBuilder.toString();
        assertTrue(!transcript.contains("Protocols\n\n["));
        assertTrue(transcript.contains("line1\n  line2"));
    }
}

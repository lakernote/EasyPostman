package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CaptureFlowTest {

    @Test
    public void shouldTrackWebSocketStreamTimeline() {
        CaptureFlow flow = new CaptureFlow(
                "GET",
                "ws://example.com/socket",
                "example.com",
                "/socket",
                Map.of("Upgrade", "websocket"),
                new byte[0]
        );
        flow.recordResponseStart(101, "Switching Protocols", Map.of("Upgrade", "websocket"));
        flow.appendRequestStreamEvent("TEXT len=5\nhello");
        flow.appendResponseStreamEvent("TEXT len=5\nworld");

        assertEquals(flow.streamEventCount(), 2);
        assertTrue(flow.requestBodyPreview().contains("hello"));
        assertTrue(flow.responseBodyPreview().contains("world"));
        assertTrue(flow.streamDetailText().contains("CLIENT"));
        assertTrue(flow.streamDetailText().contains("SERVER"));
    }

    @Test
    public void shouldAppendSseChunksToTimeline() {
        CaptureFlow flow = new CaptureFlow(
                "GET",
                "https://example.com/events",
                "example.com",
                "/events",
                Map.of("Accept", "text/event-stream"),
                new byte[0]
        );
        flow.recordResponseStart(200, "OK", Map.of("Content-Type", "text/event-stream"));
        flow.appendResponseBody("data: hello\n\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(flow.streamEventCount(), 1);
        assertTrue(flow.streamDetailText().contains("SSE"));
        assertTrue(flow.streamDetailText().contains("data: hello"));
    }

    @Test
    public void shouldShowTlsFailureDiagnosisInResponseDetails() {
        CaptureFlow flow = new CaptureFlow(
                "TLS",
                "https://pinned.example.com/",
                "pinned.example.com",
                "/",
                Map.of(),
                new byte[0]
        );

        flow.fail(495, "client rejected certificate");

        String detail = flow.responseDetailText();
        assertTrue(detail.contains("pinned.example.com"));
        assertTrue(detail.contains("client rejected certificate"));
        assertTrue(detail.contains("TLS"));
    }

    @Test
    public void shouldKeepOriginalHeadersInDetailText() {
        CaptureFlow flow = new CaptureFlow(
                "GET",
                "https://example.com/api",
                "example.com",
                "/api",
                Map.of(
                        "Authorization", "Bearer very-secret-token",
                        "Cookie", "sid=private-session",
                        "Accept", "application/json"
                ),
                new byte[0]
        );

        String detail = flow.requestDetailText();

        assertTrue(detail.contains("Authorization"));
        assertTrue(detail.contains("Cookie"));
        assertTrue(detail.contains("Accept: application/json"));
        assertTrue(detail.contains("very-secret-token"));
        assertTrue(detail.contains("private-session"));
    }
}

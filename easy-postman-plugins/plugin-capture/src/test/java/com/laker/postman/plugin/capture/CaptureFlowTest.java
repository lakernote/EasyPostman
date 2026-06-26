package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

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
        assertTrue(flow.responseDetailText().contains("world"));
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
    public void shouldShowSourceAndDiagnosticsDetails() {
        CaptureSourceInfo sourceInfo = CaptureSourceInfo.network(
                        "127.0.0.1",
                        53421,
                        "127.0.0.1",
                        8888
                )
                .withProcess("12345", "Chrome Helper", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome Helper");
        CaptureFlow flow = new CaptureFlow(
                "GET",
                "https://example.com/api",
                "example.com",
                "/api",
                Map.of(),
                new byte[0],
                "connection-1",
                sourceInfo,
                List.of(CaptureDiagnosticEvent.info(
                        CaptureDiagnosticPhase.CONNECT,
                        CaptureDiagnosticRole.CLIENT_CONNECTION,
                        "Client requested HTTPS tunnel",
                        "CONNECT example.com:443",
                        ""
                ))
        );

        flow.appendDiagnosticEvent(CaptureDiagnosticEvent.error(
                CaptureDiagnosticPhase.CLIENT_TLS,
                CaptureDiagnosticRole.SOURCE_APP,
                "Client rejected EasyPostman certificate",
                "SSLHandshakeException: unknown_ca",
                "Bypass this host or import the EasyPostman Root CA into the source app trust store."
        ));

        assertEquals(flow.toRow()[2], "Chrome Helper");
        assertEquals(flow.toRow()[3], "12345");
        assertEquals(flow.toRow()[4], "api");
        assertEquals(flow.toRow()[6], "https://example.com/api");
        String diagnostics = flow.diagnosticsDetailText();
        assertTrue(diagnostics.contains(CaptureDiagnosticRole.SOURCE_APP.displayText()));
        assertTrue(diagnostics.contains("Chrome Helper · PID 12345"));
        assertTrue(diagnostics.contains("CONNECT"));
        assertTrue(diagnostics.contains("CLIENT_TLS"));
        assertTrue(diagnostics.contains("unknown_ca"));
        assertTrue(diagnostics.contains("Bypass this host"));
    }

    @Test
    public void shouldExposeResourceTypeInCompactTableRow() {
        CaptureFlow image = new CaptureFlow(
                "GET",
                "https://example.com/assets/logo.png",
                "example.com",
                "/assets/logo.png",
                Map.of(),
                new byte[0]
        );
        CaptureFlow script = new CaptureFlow(
                "GET",
                "https://example.com/assets/app.js",
                "example.com",
                "/assets/app.js",
                Map.of(),
                new byte[0]
        );
        CaptureFlow sse = new CaptureFlow(
                "GET",
                "https://example.com/events",
                "example.com",
                "/events",
                Map.of("Accept", "text/event-stream"),
                new byte[0]
        );

        assertEquals(image.toRow()[4], "image");
        assertEquals(script.toRow()[4], "js");
        assertEquals(sse.toRow()[4], "sse");
    }

    @Test
    public void shouldIncludeSourceInRequestAndResponseDetails() {
        CaptureSourceInfo sourceInfo = CaptureSourceInfo.network(
                        "127.0.0.1",
                        53421,
                        "127.0.0.1",
                        8888
                )
                .withProcess("25562", "codex", "/usr/local/bin/codex");
        CaptureFlow flow = new CaptureFlow(
                "GET",
                "https://example.com/api",
                "example.com",
                "/api",
                Map.of(),
                new byte[0],
                "connection-1",
                sourceInfo,
                List.of()
        );

        assertTrue(flow.requestDetailText().contains(CaptureI18n.t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_SOURCE)));
        assertTrue(flow.requestDetailText().contains("codex · PID 25562"));
        assertTrue(flow.responseDetailText().contains(CaptureI18n.t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_SOURCE)));
        assertTrue(flow.responseDetailText().contains("codex · PID 25562"));
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

    @Test
    public void shouldDecodeGzipResponseBodyForPreview() throws IOException {
        CaptureFlow flow = new CaptureFlow(
                "GET",
                "https://example.com/api",
                "example.com",
                "/api",
                Map.of("Accept-Encoding", "gzip"),
                new byte[0]
        );

        flow.complete(
                200,
                "OK",
                Map.of(
                        "Content-Encoding", "gzip",
                        "Content-Type", "application/json"
                ),
                gzip("{\"message\":\"hello gzip\"}")
        );

        String detail = flow.responseDetailText();

        assertTrue(flow.responseBodyPreview().contains("hello gzip"));
        assertTrue(detail.contains(CaptureI18n.t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_RESPONSE_BODY_DECODED, "gzip")));
        assertTrue(detail.contains("hello gzip"));
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }
}

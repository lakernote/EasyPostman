package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureFlowClassifierTest {

    @Test
    public void shouldRecognizeErrorStatusAndTlsProxyFailures() {
        CaptureFlow clientError = new CaptureFlow("GET", "https://api.example.com/users",
                "api.example.com", "/users", Map.of(), null);
        clientError.complete(405, "Method Not Allowed", Map.of(), new byte[0]);

        CaptureFlow tlsFailure = new CaptureFlow("TLS", "https://pinned.example.com/",
                "pinned.example.com", "/", Map.of(), null);
        tlsFailure.fail(495, "client rejected certificate");

        assertTrue(CaptureFlowClassifier.isError(clientError));
        assertTrue(CaptureFlowClassifier.isError(tlsFailure));
    }

    @Test
    public void shouldRecognizeStaticResources() {
        CaptureFlow script = new CaptureFlow("GET", "https://static.example.com/assets/app.js",
                "static.example.com", "/assets/app.js", Map.of(), null);

        assertTrue(CaptureFlowClassifier.isStaticResource(script));
    }

    @Test
    public void shouldRecognizeTelemetryNoise() {
        CaptureFlow telemetry = new CaptureFlow("POST", "https://chat.openai.com/ces/v1/telemetry/intake",
                "chat.openai.com", "/ces/v1/telemetry/intake", Map.of(), null);
        CaptureFlow collector = new CaptureFlow("POST", "https://collector.github.com/github/collect",
                "collector.github.com", "/github/collect", Map.of(), null);

        assertTrue(CaptureFlowClassifier.isTelemetry(telemetry));
        assertTrue(CaptureFlowClassifier.isTelemetry(collector));
    }

    @Test
    public void shouldRecognizeApiTraffic() {
        CaptureFlow pathApi = new CaptureFlow("POST", "https://example.com/backend-api/events",
                "example.com", "/backend-api/events", Map.of(), null);
        CaptureFlow jsonApi = new CaptureFlow("GET", "https://example.com/users",
                "example.com", "/users", Map.of("Accept", "application/json"), null);
        CaptureFlow image = new CaptureFlow("GET", "https://example.com/logo.png",
                "example.com", "/logo.png", Map.of("Accept", "image/png"), null);

        assertTrue(CaptureFlowClassifier.isApiTraffic(pathApi));
        assertTrue(CaptureFlowClassifier.isApiTraffic(jsonApi));
        assertFalse(CaptureFlowClassifier.isApiTraffic(image));
    }
}

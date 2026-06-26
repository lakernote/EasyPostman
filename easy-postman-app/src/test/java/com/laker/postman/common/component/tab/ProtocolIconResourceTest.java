package com.laker.postman.common.component.tab;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ProtocolIconResourceTest {

    @Test
    public void protocolIconsShouldUseContrastStableFixedColors() throws IOException {
        String http = readIcon("http.svg");
        String websocket = readIcon("websocket.svg");
        String sse = readIcon("sse.svg");

        assertTrue(http.contains("fill=\"#2e7d32\""), "http.svg should use a darker HTTP green");
        assertFalse(http.contains("#4caf50"), "http.svg should not use the old bright Material green");
        assertFalse(http.contains("rgba("), "SVG stroke opacity should use SVG attributes");

        assertTrue(websocket.contains("fill=\"#3771e1\""), "websocket.svg should keep its stable protocol blue");

        assertTrue(sse.contains("fill=\"#b45309\""), "sse.svg should use a darker SSE orange");
        assertFalse(sse.contains("#f59e0b"), "sse.svg should not use the old bright orange");
    }

    private static String readIcon(String iconName) throws IOException {
        try (InputStream stream = ProtocolIconResourceTest.class.getResourceAsStream("/icons/" + iconName)) {
            assertNotNull(stream, "Missing protocol icon resource: " + iconName);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

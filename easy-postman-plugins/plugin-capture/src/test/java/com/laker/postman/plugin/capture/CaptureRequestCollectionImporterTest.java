package com.laker.postman.plugin.capture;

import com.laker.postman.model.RequestImportBodyTypes;
import com.laker.postman.model.RequestImportDraft;
import com.laker.postman.model.RequestImportProtocol;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class CaptureRequestCollectionImporterTest {

    @Test
    public void shouldMapCapturedFlowToImportDraftAndFilterTransportHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.com");
        headers.put("Accept", "application/json");
        headers.put("Content-Length", "7");
        CaptureFlow flow = new CaptureFlow(
                "POST",
                "https://example.com/api",
                "example.com",
                "/api",
                headers,
                "payload".getBytes(StandardCharsets.UTF_8)
        );

        List<RequestImportDraft> drafts = new CaptureRequestCollectionImporter(null).toDrafts(List.of(flow));

        assertEquals(drafts.size(), 1);
        RequestImportDraft draft = drafts.get(0);
        assertEquals(draft.name(), "/api");
        assertEquals(draft.url(), "https://example.com/api");
        assertEquals(draft.method(), "POST");
        assertEquals(draft.protocol(), RequestImportProtocol.HTTP);
        assertEquals(draft.bodyType(), RequestImportBodyTypes.RAW);
        assertEquals(draft.body(), "payload");
        assertEquals(draft.headers().size(), 1);
        assertEquals(draft.headers().get(0).key(), "Accept");
    }

    @Test
    public void shouldUseStableDuplicateNamesWithinOneImportBatch() {
        CaptureFlow first = new CaptureFlow("GET", "https://example.com/api", "example.com", "/api", Map.of(), null);
        CaptureFlow second = new CaptureFlow("GET", "https://example.com/api", "example.com", "/api", Map.of(), null);

        List<RequestImportDraft> drafts = new CaptureRequestCollectionImporter(null).toDrafts(List.of(first, second));

        assertEquals(drafts.get(0).name(), "/api");
        assertEquals(drafts.get(1).name(), "/api 2");
    }
}

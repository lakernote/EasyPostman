package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureSessionStoreTest {

    @Test
    public void shouldThrottleRepeatedTlsIssueRowsForSameHost() {
        CaptureSessionStore store = new CaptureSessionStore();

        boolean firstRecorded = store.recordTlsIssue("api.apple-cloudkit.com", 443, "first");
        boolean duplicateRecorded = store.recordTlsIssue("api.apple-cloudkit.com", 443, "second");

        assertTrue(firstRecorded);
        assertFalse(duplicateRecorded);
        assertEquals(store.snapshot().size(), 1);
        assertEquals(store.snapshot().get(0).method(), "TLS");
        assertEquals(store.snapshot().get(0).statusCode(), 495);
    }

    @Test
    public void shouldTrimOldestFlowsWhenMaxFlowLimitIsReached() {
        CaptureSessionStore store = new CaptureSessionStore(50);

        CaptureFlow first = null;
        CaptureFlow newest = null;
        for (int i = 1; i <= 51; i++) {
            CaptureFlow flow = store.createFlow(
                    "GET",
                    "https://example.com/" + i,
                    "example.com",
                    "/" + i,
                    Map.of(),
                    new byte[0]);
            if (i == 1) {
                first = flow;
            }
            newest = flow;
        }

        assertEquals(store.snapshot().size(), 50);
        assertEquals(store.snapshot().get(0).id(), newest.id());
        String firstFlowId = first.id();
        assertFalse(store.snapshot().stream().anyMatch(flow -> flow.id().equals(firstFlowId)));
        assertEquals(store.maxFlows(), 50);
    }
}
